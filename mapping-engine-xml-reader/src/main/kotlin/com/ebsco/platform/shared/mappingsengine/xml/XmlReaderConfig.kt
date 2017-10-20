package com.ebsco.platform.shared.mappingsengine.xml

import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.json.JsonProvider

data class XmlToRecordParserConfig(val preserveNestedTextElements_ByXPath: List<String> = emptyList(),
                                   val preserveNestedTextElements_AutoDetect: Boolean = false,
                                   val preserveNestedTextElements_UnhandledResultInError: Boolean = true,
                                   val forceSingleValueNodes_ByXPath: List<String> = emptyList(),
                                   val forceElevateTextNode_ByXPath: List<String> = emptyList(),
                                   val forceElevateTextNodesAreSingleValued: Boolean = false,
                                   val jsonProvider: JsonProvider = JacksonJsonProvider(),
                                   val textNodeName: String = "#text",
                                   val attributeNodePrefix: String = "@",
                                   val attributePrefixesToKeep: Set<String> = setOf("xmlns", "xml")) {


    companion object {
        val DEFAULTS = XmlToRecordParserConfig()

    }
}