package com.ebsco.entarch.mappings.streamsets

import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.ParseContext
import com.streamsets.pipeline.api.Field
import com.streamsets.pipeline.api.Record


// TODO: placeholders to let this build until merging in other project code from command-line tool

fun DocumentContext.processMappings() {
    /* decompiled stub */
}

fun ParseContext.registerMappingConfig(configJson: String?): ParseContext {
    /* decompiled stub */
    return this
}

fun Record.jsonPathQuery(jsonPath: String): List<Field> {
    /* decompiled stub */
    return emptyList()
}

fun Record.jsonPathQueryResultPaths(jsonPath: String): List<String> {
    /* decompiled stub */
    return emptyList()
}

