package com.ebsco.platform.shared.mappingsengine.core

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option

/**
 * MappingsEngine applies transformations mutating an existing JSON object
 *
 * All tranformations are plugged in, and are specified in-order within the configuration object
 *
 */
class MappingsEngine(val config: MappingsEngineConfig = MappingsEngineConfig.DEFAULTS) {
    private val jsonPaths = Configuration.builder()
            .options(Option.AS_PATH_LIST, Option.SUPPRESS_EXCEPTIONS)
            .jsonProvider(config.jsonProvider)
            .build()

    private val jsonValues = Configuration.builder()
            .options(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS)
            .jsonProvider(config.jsonProvider)
            .build()

    fun processDocument(jsonDocument: Any) {
        config.transformers.forEach { tx ->
            tx.apply(jsonDocument, jsonPaths, jsonValues)
        }
    }
}