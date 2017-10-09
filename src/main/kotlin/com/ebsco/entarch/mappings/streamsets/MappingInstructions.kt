package com.ebsco.entarch.mappings.streamsets

class MappingInstructions(val mappings: List<MappingStep>)

interface MappingStep {

}

data class JsonPathExpression(val expression: String)
data class JsonRelExpression(val expression: String)

enum class MergeMode {
    REPLACE_SINGLE, REPLACE_MULTIPLE, ADD_MULTPLE
}

data class RenameStep(val fromPath: JsonPathExpression, val toPath: JsonRelExpression,
                 val mergeMode: MergeMode) : MappingStep {
}

data class CopyStep(val fromPath: JsonPathExpression, val toPath: JsonRelExpression,
                    val mergeMode: MergeMode) : MappingStep {

}

