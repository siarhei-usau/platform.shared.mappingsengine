package com.ebsco.platform.shared.mappingsengine.core.transformers

import com.ebsco.platform.shared.mappingsengine.core.JsonTransformer
import com.ebsco.platform.shared.mappingsengine.core.JsonTransformerContext
import com.ebsco.platform.shared.mappingsengine.core.ResolvedPaths

class OriginalConcatJson() : JsonTransformer {
    lateinit var fromPaths: List<String>
    lateinit var delimiter: String
    lateinit var targetPath: String

    override fun apply(context: JsonTransformerContext) {
        val allMappings: List<Pair<String, List<ResolvedPaths>>> = fromPaths.map {
            Pair(it, context.origianlQueryAndResolveTargetPaths(it, targetPath, true))
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
