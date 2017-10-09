package com.ebsco.entarch.mappings.streamsets

import com.streamsets.pipeline.api.Record

interface ConfiguredManipulation {
    fun invoke(record: Record)
}

class RenameManipulation(val sourcePath: String, val targetName: String, val appendAsArray: Boolean = false) : ConfiguredManipulation {
    override fun invoke(record: Record) {
        val sourceFields = record.jsonPathQueryResultPaths(sourcePath)

        sourceFields.map { sourceField ->
            record.get(sourceField).valueAsString
        }.forEach { sourceValue ->
            record.write(targetName, sourceValue, append = appendAsArray)
        }

        sourceFields.forEach { record.delete(it) }
    }
}

class CopyManipulation(val sourcePaths: List<String>, val targetName: String, val appendAsArray: Boolean = true) : ConfiguredManipulation {
    override fun invoke(record: Record) {
        sourcePaths.map { query -> record.jsonPathQuery(query) }
                .flatten()
                .map { it.valueAsString }
                .forEach { sourceValue ->
                    record.write(targetName, sourceValue, append = appendAsArray)
                }
    }
}

class ConcatManipulation(val sourcePaths: List<String>, val delimiter: String, val targetName: String, val appendAsArray: Boolean = true) : ConfiguredManipulation {
    override fun invoke(record: Record) {
        val newValue = sourcePaths.map { query -> record.jsonPathQuery(query) }
                .flatten()
                .map { it.valueAsString }
                .joinToString(delimiter)
        record.write(targetName, newValue, append = appendAsArray)
    }
}