package com.ebsco.platform.shared.mappingsengine.config

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream
import java.io.Reader

data class MappingsEngineJsonConfig(val metadata: EngineMetadata, val transforms: List<TransformsConfig>, val configuration: SubsystemConfiguration) {
    companion object {
        fun fromJson(json: String): MappingsEngineJsonConfig {
            return jacksonObjectMapper().readValue(json)
        }

        fun fromJson(json: InputStream): MappingsEngineJsonConfig {
            return jacksonObjectMapper().readValue(json)
        }

        fun fromJson(json: Reader): MappingsEngineJsonConfig {
            return jacksonObjectMapper().readValue(json)
        }
    }
}

data class EngineMetadata(val id: String, val version: String, val primaryKey: String)

data class TransformsConfig(val type: String, val id: String?, val notes: String?, val config: Map<String, Any>) {
    inline fun <reified T : Any> bind(): T {
        return jacksonObjectMapper().convertValue(config)
    }
}

data class SubsystemConfiguration(val xml2json: Xml2JsonConfig)

data class Xml2JsonConfig(val embedLiteralXmlAtPaths: List<String> = emptyList(),
                          val autoDetectMixedContent: Boolean = false,
                          val unhandledMixedContentIsError: Boolean = true,
                          val forceSingleValueElementAtPaths: List<String> = emptyList(),
                          val forceElevateTextNodesAtPaths: List<String> = emptyList(),
                          val forceElevateTextNodesAsSingleValue: Boolean = false,
                          val textNodeName: String = "value",
                          val attributeNodePrefix: String = "",
                          val preserveAttributePrefixes: List<String> = listOf("xmlns", "xml"))

