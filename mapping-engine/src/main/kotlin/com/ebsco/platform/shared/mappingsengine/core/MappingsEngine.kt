package com.ebsco.platform.shared.mappingsengine.core

import com.ebsco.platform.shared.mappingsengine.config.TransformsConfig
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.json.JsonProvider
import kotlin.reflect.KClass

private val REGISTERED_TRANSFORMERS: Map<String, KClass<out JsonTransformer>> = mapOf(
        "rename" to RenameJsonTransform::class,
        "copy" to CopyJsonTransform::class,
        "delete" to DeleteJsonTransform::class,
        "concat" to ConcatJsonTransform::class,
        "lookup" to LookupJsonTransform::class
)

/**
 * MappingsEngine applies transformations mutating an existing JSON object
 *
 * All tranformations are plugged in, and are specified in-order within the configuration object
 *
 */
class MappingsEngine(val transforms: List<TransformsConfig>,
                     val transformerClasses: Map<String, KClass<out JsonTransformer>> = REGISTERED_TRANSFORMERS,
                     val jsonProvider: JsonProvider = JacksonJsonProvider()) {
    private val jsonPaths = Configuration.builder()
            .options(Option.AS_PATH_LIST, Option.SUPPRESS_EXCEPTIONS)
            .jsonProvider(jsonProvider)
            .build()

    private val jsonValueList = Configuration.builder()
            .options(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS)
            .jsonProvider(jsonProvider)
            .build()

    private val jsonValue = Configuration.builder()
            .options(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS)
            .jsonProvider(jsonProvider)
            .build()

    private val mapper: ObjectMapper = jacksonObjectMapper()

    private val transformSteps: List<JsonTransformer> = transforms.map { cfg ->
        val transformClass = transformerClasses[cfg.type] ?: throw IllegalStateException("Transformer type ${cfg.type} is not registered!")
        mapper.convertValue(cfg.config, transformClass.java)
    }

    fun processDocument(jsonDocument: Any) {
        val context = JsonTransformerContext(jsonDocument, jsonPaths, jsonValue, jsonValueList)
        transformSteps.forEach { tx ->
            tx.apply(context)
        }
    }
}