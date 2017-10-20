package com.ebsco.entarch.mappings.streamsets

import com.streamsets.pipeline.api.Field

fun jsonToStreamSetsField(json: Map<String, Any>): Field {
    // TODO: only needed if we decide to materialize a record instead of text version of the JSON.
    return Field.create("null")
}