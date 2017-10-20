package com.ebsco.entarch.mappings.streamsets

import com.streamsets.pipeline.api.ErrorCode
import com.streamsets.pipeline.api.GenerateResourceBundle
import com.streamsets.pipeline.api.Label


const val MAPPINGS_GROUP = "Mappings"

val systemFieldsToDelete = listOf("/fileRef")

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

const val emptyConfigJson = """
{
  "metadata": {
    "id": "empty",
    "version": "1.0.0",
    "primaryKey": "$.id"
  },
  "transforms": [
  ],
  "configuration": {
    "xml2json": {
    }
  }
}
"""