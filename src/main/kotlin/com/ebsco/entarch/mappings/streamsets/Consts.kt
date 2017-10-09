package com.ebsco.entarch.mappings.streamsets

import com.streamsets.pipeline.api.ErrorCode
import com.streamsets.pipeline.api.GenerateResourceBundle
import com.streamsets.pipeline.api.Label


const val MAPPINGS_GROUP = "Mappings"

const val FEATURE_RENAME_VALUE_TO_TEXT = false
const val FEATURE_RENAME_TEXT_TO_VALUE = true
const val FEATURE_ELEVATE_VALUE_TO_FIELD = false
const val FEATURE_XML_ATTRIBUTES_TO_FIELDS = true
const val FEATURE_ATTRIBUTES_TO_NORMAL_NAME = true

val systemFieldsToDelete = listOf("/fileRef")
val systemFieldForJsonOutput = "/json"

@GenerateResourceBundle
enum class Groups(val groupName: String) : Label {
    Mappings(MAPPINGS_GROUP);

    override fun getLabel(): String = groupName
}

@GenerateResourceBundle
enum class Errors(val msg: String) : ErrorCode {
    EBSCO_INVALID_CONFIG("Configuration is invalid because of {}");

    override fun getMessage(): String = msg
    override fun getCode(): String = name
}