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

        val DEFAULTS_WITH_ERS_TEMP_PATHS = DEFAULTS.copy(preserveNestedTextElements_ByXPath = listOf(
                // TODO: load this from configuration
                "//book[*]/body[*]/book-part[*]/book-front[*]/sec",
                "//book[*]/body[*]/book-part[*]/body[*]/sec",
                "//book[*]/body[*]/book-part[*]/book-part-meta[*]/abstract/p"
        ))
    }
}