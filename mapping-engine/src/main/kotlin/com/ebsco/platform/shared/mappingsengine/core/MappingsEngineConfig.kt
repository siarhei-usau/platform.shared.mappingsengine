package com.ebsco.platform.shared.mappingsengine.core

import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.json.JsonProvider

data class MappingsEngineConfig(val transformers: List<JsonTransformer> = emptyList(),
                                val jsonProvider: JsonProvider = JacksonJsonProvider()) {
    companion object {
        val DEFAULTS = MappingsEngineConfig()
    }
}