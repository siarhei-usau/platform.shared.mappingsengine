package com.ebsco.platform.shared.mappingsengine.core

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath

interface JsonTransformer {
    fun apply(context: JsonTransformerContext)
}

open class JsonTransformerContext(val jsonObject: Any, val jpathCfg: Configuration, val jvalueCfg: Configuration, val jvalueListCfg: Configuration) {
    val jpathCtx = JsonPath.using(jpathCfg).parse(jsonObject)           // existing objects are wrapped, not parsed
    val jvalueCtx = JsonPath.using(jvalueCfg).parse(jsonObject)         // existing objects are wrapped, not parsed
    val jvalueListCtx = JsonPath.using(jvalueListCfg).parse(jsonObject) // existing objects are wrapped, not parsed


    // we might be able to call CompiledPath.eval to get both values and paths at same time, but we lose path caching and have to do that ourselves

    fun queryForPaths(jsonPath: String): List<String> = jpathCtx.read(jsonPath)
    fun queryForPaths(jsonPath: JsonPath): List<String> = jpathCtx.read(jsonPath)

    fun queryForValues(jsonPath: String): List<Any> = jvalueListCtx.read(jsonPath)
    fun queryForValues(jsonPath: JsonPath): List<Any> = jvalueListCtx.read(jsonPath)

    fun queryForValue(jsonPath: String): Any = jvalueCtx.read(jsonPath)
    fun queryForValue(jsonPath: JsonPath): Any = jvalueCtx.read(jsonPath)

    fun resolveTargetPaths(targetJsonPath: String, relativeToPaths: List<String>, allowNonMatching: Boolean = false): List<ResolvedPaths>
            = jpathCtx.resolveTargetPaths(targetJsonPath, relativeToPaths, allowNonMatching)

    fun queryAndResolveTargetPaths(jsonPath: String, targetJsonPath: String, allowNonMatching: Boolean = false): List<ResolvedPaths>
            = resolveTargetPaths(targetJsonPath, queryForPaths(jsonPath), allowNonMatching)

    fun queryAndResolveTargetPaths(jsonPath: JsonPath, targetJsonPath: String, allowNonMatching: Boolean = false): List<ResolvedPaths>
            = resolveTargetPaths(targetJsonPath, queryForPaths(jsonPath), allowNonMatching)

    fun applyUpdate(target: ResolvedPaths, jsonFragment: Any) = jpathCtx.applyUpdatePath(target.targetBasePath, target.targetUpdatePath, jsonFragment)
    fun applyUpdate(targetBasePath: String, targetUpdatePath: String, jsonFragment: Any) = jpathCtx.applyUpdatePath(targetBasePath, targetUpdatePath, jsonFragment)

    fun deleteValue(jsonPath: JsonPath) = jvalueCtx.delete(jsonPath)
    fun deleteValue(jsonPath: String) = jvalueCtx.delete(jsonPath)
}

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
        // it gets complicated to handle cases where the source values are not in the same objects.  So we assume they are.
        //
        // Therefore we expect for every sourceJsonPath a list of matching paths of which some have target paths (if a value was present)
        //
        //
        // So to handle the city/state case where state is at a level above cities and you want to build "city, state", you would do multiple transforms:
        //     copy the state name into the city object
        //     concat the city and state within the same object
        //     remove the temp state field

        // TODO: later we can look at doing prefix matching to find the closest match from each when selecting from multiple objects and allow
        // mixed object selection:
        //
        //     $.state[0].cities[0].name  "Denver"            target match $.state[0].cities[0]+cityState
        //     $.state[0].cities[1].name  "Boulder"           target match $.state[0].cities[1]+cityState
        //     $.state[0].name            "Colorado"          none, but path $.state[0] is prefix match for above
        //     $.state[1].cities[0].name  "San Francisco"     target match $.state[1].cities[0]+cityState
        //     $.state[1].cities[1].name  "Santa Cruz"        target match $.state[1].cities[1]+cityState
        //     $.state[1].name            "California"        none, but path $.state[1] is a prefix match for above
        //

        val allMappings: List<Pair<String, List<ResolvedPaths>>> = fromPaths.map {
            Pair(it, context.queryAndResolveTargetPaths(it, targetPath, true))
        }

        val allMappingsGroupedByTarget = allMappings.map { mappedPath ->
            mappedPath.second.map {
                val key = Pair(it.targetBasePath, it.targetUpdatePath)
                Pair(key, Pair(mappedPath.first, it))
            }
        }.flatten().groupBy { it.first }.mapValues {
            it.value.map { it.second }.map { it.first to it.second.sourcePath }.toMap()
        }

        // we now have a map of target path, to a map of source paths to actual value path (in the resolved object)

        // for each target path, collect the values in the order of the original source path list
        allMappingsGroupedByTarget.forEach { (targetPath, sourceMap) ->
            val sourceValuesInOrder = fromPaths.map { originalPath ->
                sourceMap[originalPath]?.let { context.queryForValue(it) }
            }.filterNotNull()
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