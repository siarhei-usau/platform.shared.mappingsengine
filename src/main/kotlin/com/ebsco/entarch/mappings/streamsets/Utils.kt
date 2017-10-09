package com.ebsco.entarch.mappings.streamsets

import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.ParseContext
import com.streamsets.pipeline.api.Field
import com.streamsets.pipeline.api.Record

























































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




val one = RenameManipulation("","")
val two = CopyManipulation(listOf(""),"")
val three = ConcatManipulation(listOf(""),",","")