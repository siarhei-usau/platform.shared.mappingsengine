package com.ebsco.platform.shared.mappingsengine.core

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option

/**
 * Target Pathing:  resolving relative insertion paths.
 *
 * Those starting with `@.` are relative to the found object, and use normal JSON Path Syntax to path downwards
 *          @.b.c.d.e.f
 *
 * Where any path elements must exist.  For elements that can be added if missing, a `+` replaces the `.` for the
 * point at which new paths can be created:
 *
 *          @.b.c+d.e.f
 *
 * allows `d` to be added, then `e`, and then property `f` with whatever value is being set.  Existing elements
 * are traversed instead of being inserted.
 *
 * To path upwards, use the `^` instead of the `.` and then resume downward with `.` or `+`.  For example:
 *
 *          @^y^x+d.e.f
 *
 * Would go up from the current node to a parent called `y`, its parent `x` and then allow `d` to be created and
 * so on downwards.
 *
 * A relative path through an array index, has the following behavior:  If pathing through a parent array with `[*]`
 * then the index used will be the same as that within the path of the current node.  for example, current node is:
 *
 *          $.b.c[1].d[2].e.f
 *
 * the relative path from `f` of `@^e^d[*]+z` or `@^e^d+z` would set insertion point at `$.a.c[1].d[2].z`
 *
 * An array must exist for the existing portion of the target path, or it is an invalid target. After the update
 * marker of `+` the bahavior is:
 *
 *          [*]  all indexes that match at that point are updated (but not added)
 *          [*+]  all indexes that match at that point are updated, if not present, an empty object is added
 *          [+]  a new item will be added to the array at this point regardless of contents
 *          [0]  the first item is updated (but not added)
 *          [0+] the first item is updated and if not present, an empty object added
 *
 * After the update marker `+` you can only use simple pathing, no expressions can be used.  They can be used to
 * the left of the marker to indicate a query for a path that already exists.
 *
 */
fun DocumentContext.resolveTargetPaths(targetPath: String, matchingPaths: List<String>): List<Triple<String, String, String>> {
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
            // after popping up, we can only go downwards.

            val firstDot = existingPath.indexOf('.')
            if (existingPath[1] != '^' || (firstDot != -1 && existingPath.indexOf('^', firstDot) != -1)) {
                throw IllegalArgumentException("Relative path $targetPath must have ^ as second character and all ^ before the first .")
            }

            val upwardsParts = existingPath.substring(2).substringBefore(".").split('^')
            val downwardsPart = existingPath.substringAfter(".", "")

            // go up for each path for each upward part
            val uppedPaths = matchingPaths.map { matchPath ->
                val temp: String = matchPath.substring(1) // skip the $
                var tempParts = temp.split(']').map { it.removePrefix("[") }.filter { it.isNotBlank() }
                var lastPath: String? = null
                upwardsParts.forEach { upper ->
                    tempParts = tempParts.dropLast(1)
                    lastPath = tempParts.map { "[$it]" }.joinToString("", "$")
                    val lastPart = tempParts.lastOrNull()?.let {
                        if (numRegex.matches(it)) {
                            tempParts = tempParts.dropLast(1)
                            tempParts.lastOrNull()
                        } else {
                            it
                        }
                    }
                    if (lastPart?.startsWith('\'') ?: false) {
                        val id = tempParts.last().trim('\'')
                        if (id != upper) {
                            throw IllegalStateException("Cannot path upwards using $targetPath from starting $matchPath, error popping up to $upper, found $id instead")
                        }
                    } else {
                        throw IllegalStateException("Cannot path upwards using $targetPath from starting $matchPath")
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
            if (existingPath[1] != '.') throw IllegalStateException("Expected '.' as second char in @ relative path, $targetPath")
            // current node and down
            val downwardIsEasy = matchingPaths.map { Pair(it, "$it.${existingPath.substring(2)}") }
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
    if (matchingPathsMissing.isNotEmpty()) {
        throw IllegalStateException("Some source paths cannot be related to a root existing path: ${matchingPathsMissing}")
    }

    return correlatedMatchWithTarget.map { Triple(it.first, it.second!!, updatePath) }
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

        // must handle:
        //    a.b.c
        //    a.b[x].c
        //
        //     where x is one of *, *+, 0, 0+, +


        fun drillDownToUpdate(startNode: Any, steppy: Iterator<Pair<String, Boolean>>) {
            var node = startNode
            while (steppy.hasNext()) {
                val (step, last) = steppy.next()

                val id = step.substringBefore('[')
                val idx = step.substringAfter('[', "").removeSuffix("]")

                if (!JSON.isMap(node)) throw IllegalStateException("Update pathing through $id from $baseNode / $updatePath found something other than a Map at $id")


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
                    val arrayNodeExists = id in JSON.getPropertyKeys(node)
                    val checkArrayNode = if (arrayNodeExists) JSON.getMapValue(node, id) else null
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
                            (0..arraySize-1).forEach { i ->
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

        val stepIterator = updateSteps.mapIndexed { stepNum, step -> Pair(step, stepNum == updateSteps.size - 1) }.iterator()
        drillDownToUpdate(baseNode!!, stepIterator)
    }


}