package com.ebsco.entarch.mappings.streamsets

import com.streamsets.pipeline.api.Field
import com.streamsets.pipeline.api.Record
import com.streamsets.pipeline.api.impl.Utils

fun parseRecordPath(path: String): List<PathStep> {
        val fieldPath = EscapeUtil.standardizePathForParse(path, true)
        val elements = mutableListOf(PathStep.ROOT)

        if (!fieldPath.isEmpty()) {
            val chars = fieldPath.toCharArray()
            var requiresStart = true
            var requiresName = false
            var requiresIndex = false
            var singleQuote = false
            var doubleQuote = false
            val collector = StringBuilder()
            var pos = 0

            endlessly@ while (true) {
                if (pos >= chars.size) {
                    if (singleQuote || doubleQuote) {
                        throw IllegalArgumentException(Utils.format("Invalid fieldPath '{}' at char '{}' ({})", *arrayOf(fieldPath, 0, "quotes are not properly closed")))
                    }

                    if (pos < chars.size) {
                        throw IllegalArgumentException(Utils.format("Invalid fieldPath '{}' at char '{}'", *arrayOf(fieldPath, pos)))
                    }

                    if (collector.length > 0) {
                        elements.add(PathStep.createMapElement(collector.toString()))
                    }
                    break
                }

                if (requiresStart) {
                    requiresStart = false
                    requiresName = false
                    requiresIndex = false
                    singleQuote = false
                    doubleQuote = false
                    when (chars[pos]) {
                        '/' -> requiresName = true
                        '[' -> requiresIndex = true
                        else -> throw IllegalArgumentException(Utils.format("Invalid fieldPath '{}' at char '{}' ({})", *arrayOf(fieldPath, 0, "field path needs to start with '[' or '/'")))
                    }
                } else if (requiresName) {
                    when (chars[pos]) {
                        '"' -> if (pos != 0 && chars[pos - 1] == '\\') {
                            collector.setLength(collector.length - 1)
                            collector.append(chars[pos])
                        } else if (!singleQuote) {
                            doubleQuote = !doubleQuote
                        } else {
                            collector.append(chars[pos])
                        }
                        '\'' -> if (pos != 0 && chars[pos - 1] == '\\') {
                            collector.setLength(collector.length - 1)
                            collector.append(chars[pos])
                        } else if (!doubleQuote) {
                            singleQuote = !singleQuote
                        } else {
                            collector.append(chars[pos])
                        }
                        '/', '[', ']' -> if (!singleQuote && !doubleQuote) {
                            if (chars.size <= pos + 1) {
                                throw IllegalArgumentException(Utils.format("Invalid fieldPath '{}' at char '{}' ({})", *arrayOf(fieldPath, pos, "field name can't be empty")))
                            }

                            if (chars[pos] == chars[pos + 1]) {
                                collector.append(chars[pos])
                                ++pos
                            } else {
                                elements.add(PathStep.createMapElement(collector.toString()))
                                requiresStart = true
                                collector.setLength(0)
                                --pos
                            }
                        } else {
                            collector.append(chars[pos])
                        }
                        else -> collector.append(chars[pos])
                    }
                } else if (requiresIndex) {
                    when (chars[pos]) {
                        '*', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> collector.append(chars[pos])
                        ']' -> {
                            val indexString = collector.toString()

                            try {
                                var index = 0
                                if ("*" != indexString) {
                                    index = Integer.parseInt(indexString)
                                }

                                if (index < 0) {
                                    throw IllegalArgumentException(Utils.format("Invalid fieldPath '{}' at char '{}'", *arrayOf(fieldPath, pos)))
                                }

                                elements.add(PathStep.createArrayElement(index))
                                requiresStart = true
                                collector.setLength(0)
                            } catch (var13: NumberFormatException) {
                                throw IllegalArgumentException(Utils.format("Invalid fieldPath '{}' at char '{}' ('{}' needs to be a number or '*')", *arrayOf(fieldPath, pos, indexString)), var13)
                            }
                        }
                        else -> throw IllegalArgumentException(Utils.format("Invalid fieldPath '{}' at char '{}' ({})", *arrayOf(fieldPath, pos, "only numbers and '*' allowed between '[' and ']'")))
                    }
                }

                ++pos
            }
        }

        return elements
}

enum class PathStepType {
    ROOT, ELEMENT, ARRAYINDEX
}

fun List<PathStep>.pathAsString(): String {
    val result = StringBuilder("")
    forEach {
        when (it.type) {
            PathStepType.ROOT -> result.append("")
            PathStepType.ELEMENT -> {
                result.append('/')
                result.append(EscapeUtil.singleQuoteEscape(it.name))
            }
            PathStepType.ARRAYINDEX -> {
                result.append("[${it.arrayIndex ?: 0}]")
            }
        }
    }
    return result.toString()
}

fun List<PathStep>.parentPath(): List<PathStep> = dropLast(1)

data class PathStep(val name: String?, val type: PathStepType, val arrayIndex: Int? = null) {
    companion object {
        val ROOT = PathStep(null, PathStepType.ROOT)

        fun createMapElement(name: String): PathStep {
            return PathStep(name, PathStepType.ELEMENT)
        }

        fun createArrayElement(idx: Int): PathStep {
            return PathStep(null, PathStepType.ARRAYINDEX, idx)
        }
    }
}

fun Record.query(path: String): List<Pair<String, Field>> {
    // a query is things like
    //    some/thing[*]/has[0]
    var currentPath = ""
    var currentField = get("/")?.takeIf { it.type == Field.Type.MAP } ?: throw IllegalStateException("No root map element!")
    val parts = path.split('/').filter { it.isNotBlank() }
    val lastPart = parts.size - 1


   return emptyList()
}

private fun Record.applyRenameMapping(fromPath: String, toPath: String) {
    val sourceFields = query(fromPath)

}

internal fun Record.ensureFieldPathIsSettable(path: String) {
    var currentPath = ""
    var currentField = get("/")?.takeIf { it.type == Field.Type.MAP } ?: throw IllegalStateException("No root map element!")

    val parts = path.split('/').filter { it.isNotBlank() }
    val lastPart = parts.size - 1

    parts.forEachIndexed { idx, step ->
        val isLast = idx == lastPart
        val baseName = step.substringBefore('[')
        val arrayIndex = step.substringAfter('[', "").removeSuffix("]").takeIf { it.isNotBlank() }?.toInt()

        if (arrayIndex == null) {
            // we have a map key instead of array index, and we only act on that if it is in the middle of a path
            if (!isLast) {
                // so we are at CURRENT in the middle of a path
                //    something/other/CURRENT/...
                //    something/other[0]/CURRENT/...
                val newFieldName = "$currentPath/$baseName"
                currentField = get(newFieldName) ?: Field.create(mutableMapOf<String, Field>()).also { set(newFieldName, it) }
                if (currentField.type != Field.Type.MAP) {
                    throw IllegalStateException("Existing field $newFieldName should be of type ELEMENT and is instead ${currentField.type}")
                }
                currentPath = newFieldName
            }
        } else {
            // we are a list element, and lists can only contain maps for us, so we are one of
            //     something/other/CURRENT[N]/...
            //     something/other/CURRENT[N]
            //     something/other[0]/CURRENT[N]/...
            //     something/other[0]/CURRENT[N]

            val newFieldName = "$currentPath/$baseName"
            val newFieldList = get(newFieldName) ?: Field.create(mutableListOf<Field>()).also { set(newFieldName, it) }
            if (newFieldList.type != Field.Type.LIST) {
                throw IllegalStateException("Existing field $newFieldName should be of type ARRAYINDEX and is instead ${currentField.type}")
            }

            // make sure we have enough elements to include this index
            val contents = newFieldList.valueAsList
            while (contents.size < arrayIndex+1) {
                contents.add(Field.create(mutableMapOf<String, Field>()))
            }

            val newIndexedFieldName = "$newFieldName[$arrayIndex]"
            currentField = get(newIndexedFieldName) ?: throw IllegalStateException("Missing field $newIndexedFieldName")
            if (currentField.type != Field.Type.MAP) {
                throw IllegalStateException("Field $newIndexedFieldName should be of type ELEMENT and is instead ${currentField.type}")
            }
            currentPath = newIndexedFieldName
        }
    }
}

fun Field.addAllAttributes(attr: Map<String, String>): Field {
    return this.apply {
        attr.forEach { this.setAttribute(it.key, it.value) }
    }
}

fun Record.write(path: String, value: String, fieldAttributes: Map<String, String> = emptyMap(), append: Boolean = false): Field {
    ensureFieldPathIsSettable(path)
    return Field.create(value).addAllAttributes(fieldAttributes).also { this.set(path, it) }
}

fun Record.writeAttr(path: String, fieldAttributes: Map<String, String> = emptyMap()): Field {
    ensureFieldPathIsSettable(path)
    return this.get(path).addAllAttributes(fieldAttributes)
}
