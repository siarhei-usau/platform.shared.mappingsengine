package com.ebsco.platform.shared.mappingsengine.core

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath


class RenameJsonTransform(val fromPath: String, val targetPath: String) : JsonTransformer {
    val compiledSourceJsonPath = JsonPath.compile(fromPath)

    override fun apply(context: JsonTransformerContext) {
        val fromToMapping: List<ResolvedPaths> = context.queryAndResolveTargetPaths(compiledSourceJsonPath, targetPath)
        fromToMapping.forEach { mapping ->
            val sourceValue = context.queryForValue(mapping.sourcePath)
            context.applyUpdate(mapping, sourceValue)
            context.deleteValue(mapping.sourcePath)
        }
    }
}

class DeleteJsonTransform(val deletePath: String) : JsonTransformer {
    val compiledSourceJsonPath = JsonPath.compile(deletePath)
    override fun apply(context: JsonTransformerContext) {
        context.queryForPaths(compiledSourceJsonPath).forEach { context.deleteValue(it) }
    }
}


class CopyJsonTransform(val fromPath: String, val targetPath: String) : JsonTransformer {
    val compiledSourceJsonPath = JsonPath.compile(fromPath)

    override fun apply(context: JsonTransformerContext) {
        val fromToMapping: List<ResolvedPaths> = context.queryAndResolveTargetPaths(compiledSourceJsonPath, targetPath)
        fromToMapping.forEach { mapping ->
            val sourceValue = context.queryForValue(mapping.sourcePath)
            context.applyUpdate(mapping, sourceValue)
        }
    }
}

class ConcatJsonTransform(val fromPaths: List<String>, val delimiter: String, val targetPath: String) : JsonTransformer {
    override fun apply(context: JsonTransformerContext) {
        val allMappings: List<Pair<String, List<ResolvedPaths>>> = fromPaths.map {
            Pair(it, context.queryAndResolveTargetPaths(it, targetPath, true))
        }

        val (mappedSources, unmappedSources) = allMappings.partition { it.second.isNotEmpty() }

        val allMappingsGroupedByTarget = mappedSources.map { mappedPath ->
            mappedPath.second.map {
                val key = Pair(it.targetBasePath, it.targetUpdatePath)
                Pair(key, Pair(mappedPath.first, it))
            }
        }.flatten().groupBy { it.first }.mapValues {
            it.value.map { it.second }.map {
                it.first to it.second.sourcePath
            }.groupBy { it.first }.mapValues { it.value.map { it.second } }
        }

        // we now have a map of target path, to a map of source paths to actual value path (in the resolved object)

        // for each target path, collect the values in the order of the original source path list
        allMappingsGroupedByTarget.forEach { (targetPath, sourceMap) ->
            val sourceValuesInOrder = fromPaths.map { originalPath ->
                sourceMap[originalPath]?.map { context.queryForValue(it) }
            }.filterNotNull().flatten()
            context.applyUpdate(targetPath.first, targetPath.second, sourceValuesInOrder.joinToString(delimiter))
        }

    }
}

class LookupJsonTransform(val lookupResource: String, val filters: List<LookupFilter>, val mode: String = "merge",
                          val targetPath: String, val jsonTemplate: Map<String, Any>) : JsonTransformer {
    override fun apply(context: JsonTransformerContext) {
        // TODO, port from prototype
    }
}

data class LookupFilter(val lookupField: String, val fromPath: String?, val lookupValues: List<String> = emptyList())