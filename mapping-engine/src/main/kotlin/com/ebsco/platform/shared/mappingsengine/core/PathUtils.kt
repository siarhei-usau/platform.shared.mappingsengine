package com.ebsco.platform.shared.mappingsengine.core

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option

data class ResolvedPaths(val sourcePath: String, val targetBasePath: String, val targetUpdatePath: String)

// TODO: clean up path parsing with more formal parser than string splits
//       then stay with parsed paths until the very end, and render
//       This will prevent going to from string paths a few times in some cases

fun DocumentContext.resolveTargetPaths(targetPath: String, matchingPaths: List<String>, allowNoMatchingTarget: Boolean = false): List<ResolvedPaths> {
    val tempConfig = Configuration.builder()
            .options(Option.SUPPRESS_EXCEPTIONS)
            .options(Option.AS_PATH_LIST)
            .jsonProvider(this.configuration().jsonProvider())
            .mappingProvider(this.configuration().mappingProvider())
            .build()

    val numRegex = """\d+""".toRegex()
    if (targetPath.isBlank()) throw IllegalStateException("A target path cannot be blank")

    // two types of paths:
    //
    //   absolute:   $.a.b.c[*].d[0].e[?(@.filter)].f
    //   relative:   @^e^d.z
    //   relative:   @.x.y.z
    //
    // both can have one update marker '+' in the path indicating that the path to the left must exist, and the
    // remainder of the path to the right may be inserted.
    //
    //   absolute:   $a.b.c[*]+d.e.f
    //   relative:   @^y^x+d[0+].e.f
    //   relative:   @.x+y.z

    val cleanPath = targetPath.trim()
    if (!cleanPath.startsWith('$') && !cleanPath.startsWith('@')) {
        throw IllegalArgumentException("targetPath must start with \$ absolute, or @ relative path marker ${targetPath}")
    }

    val existingPath = cleanPath.substringBefore('+')
    val updatePath = cleanPath.substringAfter('+', "")

    val correlatedMatchWithTarget: List<Pair<String, String?>> = if (existingPath.startsWith('$')) {
        val existingCompiled = JsonPath.compile(existingPath)
        val existingPrefixes: List<String> = existingCompiled.read(this.json<Any>(), tempConfig)

        // compare found nodes against matching source nodes, we need a match for each of them
        val temp = matchingPaths.map { match ->
            existingPrefixes.filter { match.startsWith(it) || it.startsWith(match) }.map {
                Pair(match, it)
            }
        }.flatten()
        temp
    } else if (existingPath.startsWith('@')) {

        if (existingPath == "@") {
            // special case of @+foo would result in only @ as existing path
            matchingPaths.map { Pair(it, it) }
        } else if ('^' in existingPath) {
            // we have @^a^b.c.d
            // after popping up, we can only go downwards.  So start with all the upward movement...

            val firstDot = existingPath.indexOf('.')
            if (existingPath[1] != '^' || (firstDot != -1 && existingPath.indexOf('^', firstDot) != -1)) {
                throw IllegalArgumentException("Relative path $targetPath must have ^ as second character and all ^ before the first .")
            }

            val startingPath = existingPath.substring(2)

            // and now . or [] starts downwards again...

            val earliestDownSymbol = startingPath.indexOfAny(charArrayOf('.', '['))

            val upwardsParts = (if (earliestDownSymbol >= 0) startingPath.substring(0, earliestDownSymbol) else startingPath).split('^')
            val downwardsPart = if (earliestDownSymbol >= 0) startingPath.substring(earliestDownSymbol).removePrefix(".") else ""

            // go up for each path for each upward part
            val uppedPaths = matchingPaths.map { matchPath ->
                val temp = matchPath.substring(1) // skip the $
                var tempParts = temp.split(']').map { it.removePrefix("[") }.filter { it.isNotBlank() }
                var lastPath: String? = null
                upwardsParts.forEach { upper ->
                    // going up from a.b[1].c goes to b[1],
                    //         is ['a']['b'][1]['c'] to ['a']['b'][1]
                    // going up from a.b[1] goes to a,
                    //         is ['a']['b'][1] to ['a']
                    // going up from a.b[1].c[1] goes to b[1]
                    //         is ['a']['b'][1]['c'][1] to ['a']['b'][1]

                    val inspectPossibleArrayIndex = tempParts.lastOrNull() ?: throw IllegalStateException("Cannot path upwards using $targetPath from starting $matchPath, attempted to pop up past the first element")

                    // we are on an array index, so from perspective of popping up, start at the array
                    if (numRegex.matches(inspectPossibleArrayIndex)) {
                        tempParts = tempParts.dropLast(1)
                        // ['a']['b'][1]  is now ['a']['b']

                        if (tempParts.isEmpty()) {
                            throw IllegalStateException("Cannot path upwards using $targetPath from starting $matchPath, attempted to pop up past the first element")
                        }
                    }

                    // pop up to the possible landing point
                    tempParts = tempParts.dropLast(1)
                    lastPath = tempParts.map { "[$it]" }.joinToString("", "$")

                    // the ID of the landing point is this node if it is not an array index, otherwise is the node above
                    val inspectLastPartAgain = tempParts.lastOrNull() ?: throw IllegalStateException("Cannot path upwards using $targetPath from starting $matchPath, attempted to pop up past the first element")

                    val checkPoint = if (numRegex.matches(inspectLastPartAgain)) {
                        if (tempParts.size < 2) {
                            throw IllegalStateException("Cannot path upwards using $targetPath from starting $matchPath, unexpected array index as first element")
                        } else {
                            tempParts[tempParts.size - 2]
                        }
                    } else {
                        inspectLastPartAgain
                    }

                    if (checkPoint.startsWith('\'')) {
                        val id = checkPoint.trim('\'')
                        if (id != upper) {
                            throw IllegalStateException("Cannot path upwards using $targetPath from starting $matchPath, error popping up to $upper, found $id instead")
                        }
                    } else {
                        throw IllegalStateException("Cannot path upwards using $targetPath from starting $matchPath, the next part has no valid name")
                    }
                }
                Pair(matchPath, lastPath)
            }

            if (downwardsPart.isBlank()) {
                uppedPaths
            } else {
                val downwardIsEasy = uppedPaths.map { Pair(it.first, "${it.second}.${downwardsPart}") }
                val temp = downwardIsEasy.map { (matchPath, testPath) ->
                    val existingCompiled = JsonPath.compile(testPath)
                    val existingPrefixes: List<String> = existingCompiled.read(this.json<Any>(), tempConfig)

                    if (existingPrefixes.isEmpty()) {
                        listOf(Pair(matchPath, null))
                    } else {
                        // pathing could result in more than one target point per source path
                        existingPrefixes.map { Pair(matchPath, it) }
                    }
                }.flatten()
                temp
            }
        } else {
            if (existingPath[1] != '.' && existingPath[1] != '[') throw IllegalStateException("Expected '.' or `[` as second char in @ relative path, $targetPath")
            // current node and down
            val downwardIsEasy = matchingPaths.map { Pair(it, "$it${existingPath.substring(1)}") }
            val temp = downwardIsEasy.map { (matchPath, testPath) ->
                val existingCompiled = JsonPath.compile(testPath)
                val existingPrefixes: List<String> = existingCompiled.read(this.json<Any>(), tempConfig)

                if (existingPrefixes.isEmpty()) {
                    listOf(Pair(matchPath, null))
                } else {
                    // pathing could result in more than one target point per source path
                    existingPrefixes.map { Pair(matchPath, it) }
                }
            }.flatten()
            temp
        }
    } else {
        throw IllegalStateException("targetPath must start with \$ absolute, or @ relative path marker ${targetPath}")
    }

    val matchingPathsMissing = correlatedMatchWithTarget.filter { it.second == null }
    if (!allowNoMatchingTarget && matchingPathsMissing.isNotEmpty()) {
        throw IllegalStateException("Some source paths cannot be related to a root existing path: ${matchingPathsMissing}")
    }

    val normalizedPaths = correlatedMatchWithTarget.map {
        val originalTarget = it.second ?: ""
        val normalizedTarget = if (updatePath.startsWith('[') && originalTarget.isNotBlank()) {
            // we are possibly changing an array index in place
            val tempParts = originalTarget.substring(1).split(']').map { it.removePrefix("[") }.filter { it.isNotBlank() }
            val checkLastPart = tempParts.last()
            val finalParts = if (numRegex.matches(checkLastPart)) {
                tempParts.dropLast(1)
            } else {
                tempParts
            }
            finalParts.map { "[$it]" }.joinToString("", "$")
        } else {
            originalTarget
        }
        ResolvedPaths(it.first, normalizedTarget, updatePath)
    }

    return normalizedPaths
}

fun DocumentContext.applyUpdatePath(basePath: String, updatePath: String, jsonFragment: Any) {
    val JSON = this.configuration().jsonProvider()

    val tempConfig = Configuration.builder()
            .options(Option.SUPPRESS_EXCEPTIONS)
            .options(Option.ALWAYS_RETURN_LIST)
            .jsonProvider(JSON)
            .mappingProvider(this.configuration().mappingProvider())
            .build()

    val startingPath = JsonPath.compile(basePath)

    if (updatePath.isBlank()) {
        startingPath.set<Any>(this.json<Any>(), jsonFragment, tempConfig)
    } else {
        val baseNode: Any? = startingPath.read<List<Any>>(this.json<Any>(), tempConfig).firstOrNull() ?:
                throw IllegalStateException("Base path for update $basePath was not found in document")

        val updateSteps = updatePath.trim().split('.')

        fun drillDownToUpdate(startNode: Any, originalSteps: List<Pair<String, Boolean>>) {
            var node = startNode
            var steppy = originalSteps
            while (steppy.isNotEmpty()) {
                val (step, last) = steppy.first()
                steppy = steppy.drop(1)

                val id = if (step.startsWith('[')) "" else step.substringBefore('[')
                val idx = step.substringAfter('[', "").removeSuffix("]")

                val silentArrayIndex = id.isBlank() && idx.isNotBlank()

                if (silentArrayIndex && !JSON.isArray(node)) {
                    throw IllegalStateException("Update pathing through [$idx] from $baseNode / $updatePath found something other than an Array at current point")
                }
                if (!silentArrayIndex && !JSON.isMap(node)) {
                    throw IllegalStateException("Update pathing through $id from $baseNode / $updatePath found something other than a Map at $id")
                }

                if (idx.isBlank()) {
                    // map or property
                    if (last) {
                        JSON.setProperty(node, id, jsonFragment)
                    } else {
                        if (id !in JSON.getPropertyKeys(node)) {
                            JSON.setProperty(node, id, JSON.createMap())
                        } else {
                            node = JSON.getMapValue(node, id)
                        }
                    }
                } else {
                    val checkArrayNode = if (silentArrayIndex) node else if (id in JSON.getPropertyKeys(node)) JSON.getMapValue(node, id) else null
                    if ((checkArrayNode == null && (idx == "*" || idx == "0"))
                            || (checkArrayNode != null && !JSON.isArray(checkArrayNode))) {
                        // TODO: die here or let it go?  (fail silently?)
                        throw IllegalStateException("Expected array at $id in $baseNode / $updatePath during update traversal")
                    }
                    val arrayNode = if (checkArrayNode != null) {
                        checkArrayNode
                    } else {
                        JSON.createArray().apply { JSON.setProperty(node, id, this) }
                    }

                    val arraySize = JSON.length(arrayNode)

                    when (idx) {
                        "*", "*+" -> {
                            (0..arraySize - 1).forEach { i ->
                                if (last) {
                                    JSON.setArrayIndex(arrayNode, i, jsonFragment)
                                } else {
                                    drillDownToUpdate(JSON.getArrayIndex(arrayNode, i), steppy)
                                }
                            }

                            if (arraySize == 0 && idx.endsWith('+')) {
                                if (last) {
                                    JSON.setArrayIndex(arrayNode, arraySize, jsonFragment)
                                } else {
                                    val emptyItem = JSON.createMap()
                                    JSON.setArrayIndex(arrayNode, arraySize, emptyItem)
                                    drillDownToUpdate(emptyItem, steppy)
                                }
                            }

                        }
                        "0", "0+" -> {
                            if (last) {
                                JSON.setArrayIndex(arrayNode, 0, jsonFragment)
                            } else {
                                drillDownToUpdate(JSON.getArrayIndex(arrayNode, 0), steppy)
                            }

                            if (arraySize == 0 && idx.endsWith('+')) {
                                if (last) {
                                    JSON.setArrayIndex(arrayNode, arraySize, jsonFragment)
                                } else {
                                    val emptyItem = JSON.createMap()
                                    JSON.setArrayIndex(arrayNode, arraySize, emptyItem)
                                    drillDownToUpdate(emptyItem, steppy)
                                }
                            }
                        }
                        "+" -> {
                            if (last) {
                                JSON.setArrayIndex(arrayNode, arraySize, jsonFragment)
                            } else {
                                val emptyItem = JSON.createMap()
                                JSON.setArrayIndex(arrayNode, arraySize, emptyItem)
                                drillDownToUpdate(emptyItem, steppy)
                            }
                        }
                        else -> throw IllegalStateException("Update pathing contains invalid array modifier, $baseNode / $updatePath")
                    }

                }
            }
        }

        val steps = updateSteps.mapIndexed { stepNum, step -> Pair(step, stepNum == updateSteps.size - 1) }
        drillDownToUpdate(baseNode!!, steps)
    }


}