package com.ebsco.entarch.mappings.streamsets

import _ss_com.fasterxml.jackson.databind.ObjectMapper
import _ss_com.streamsets.datacollector.json.JsonRecordWriterImpl
import com.ebsco.platform.shared.mappingsengine.core.MappingsEngine
import com.ebsco.platform.shared.mappingsengine.core.MappingsEngineConfig
import com.ebsco.platform.shared.mappingsengine.xml.XmlToRecordParser
import com.ebsco.platform.shared.mappingsengine.xml.XmlToRecordParserConfig
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.streamsets.pipeline.api.*
import com.streamsets.pipeline.api.base.SingleLaneRecordProcessor
import com.streamsets.pipeline.api.ext.json.Mode
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.StringWriter

@StageDef(version = 1, label = "XML2JSON Canonical Processor", description = "", icon = "default.png", onlineHelpRefUrl = "")
@ConfigGroups(Groups::class)
@GenerateResourceBundle
class XmlToJsonCanonicalProcessor : SingleLaneRecordProcessor() {
    val LOG = LoggerFactory.getLogger(XmlToJsonCanonicalProcessor::class.java)
    @ConfigDef(required = true, type = ConfigDef.Type.TEXT, defaultValue = """{ "mappings": [] }""",
            label = "Mappings JSON", displayPosition = 10, group = MAPPINGS_GROUP,
            description = "Mappings instruction set as JSON, either Simple or SLS formats")
    @JvmField
    var mappingInstructions: String? = null

    @ConfigDef(required = true, type = ConfigDef.Type.MODEL,
            defaultValue = """/fileRef""",
            label = "Raw XML Field", displayPosition = 5, group = MAPPINGS_GROUP,
            description = "Raw XML Field, can be fileRef or string field containing XML")
    @FieldSelectorModel(singleValued = true)
    @JvmField
    var rawXmlField: String? = null

    override fun init(): MutableList<Stage.ConfigIssue> {
        return super.init().also { issues ->
            if (mappingInstructions == "invalidValue") {
                issues.add(context.createConfigIssue(Groups.Mappings.groupName, XmlToJsonCanonicalProcessor::mappingInstructions.name,
                        Errors.EBSCO_INVALID_CONFIG, "mappings mappingInstructions JSON is invalid")) // TODO: add errors from mapping system
            }
            if (rawXmlField == "invalidValue") {
                issues.add(context.createConfigIssue(Groups.Mappings.groupName, XmlToJsonCanonicalProcessor::rawXmlField.name,
                        Errors.EBSCO_INVALID_CONFIG, "field name for XML input is invalid")) // TODO: add errors from mapping system
            }
        }
    }

    override fun destroy() {
        super.destroy()
    }

    fun String.mustStartWith(s: String) = if (this.startsWith(s)) this else s + this

    fun fixXmlToBetterJson(record: Record, inputFieldName: String = rawXmlField!!): Record {
        val inputFieldNameFixed = inputFieldName.takeIf { it.isNotBlank() }?.mustStartWith("/")
                ?: "/fileRef"
        val inputField = record.get(inputFieldNameFixed)

        val inputStream = if (inputField.type == Field.Type.STRING) {
            inputField.valueAsString.toByteArray().inputStream()
        } else {
            inputField.valueAsFileRef.createInputStream(context, InputStream::class.java)
        }

        val parser = XmlToRecordParser(XmlToRecordParserConfig.DEFAULTS_WITH_ERS_TEMP_PATHS.copy(
                preserveNestedTextElements_AutoDetect = false,
                preserveNestedTextElements_UnhandledResultInError = true,
                jsonProvider = JacksonJsonProvider(),
                textNodeName = "value",
                attributeNodePrefix = ""
        ))

        val mappings = MappingsEngine(MappingsEngineConfig.DEFAULTS.copy(jsonProvider = parser.config.jsonProvider))

        val (xmlField, xmlData) = inputStream.use { parser.parse(it) }

        @Suppress("UNCHECKED_CAST")
        val jsonObject = mapOf(xmlField to xmlData as Map<String, Any>)

        mappings.processDocument(jsonObject)

        // we go direct to a JSON field, which can be parsed by streamsets if desired to be operated upon further.

        val json = ObjectMapper().let { mapper ->
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject)
        }
        record.set(systemFieldForJsonOutput, Field.create(json)) // TODO: make this field configurable


        /* TODO: Old idea, create a full record structure into StreamSets, but the JsonProcessor can do that already so no need here
        ...

        record.set("/${xmlField}", jsonToStreamSetsField(jsonObject[xmlField]!!))

        // remove field's that break serialization
        systemFieldsToDelete.forEach { record.delete(it) }

        // temporarily remove the input XML field
        val tempXmlInput = record.delete(inputFieldNameFixed)

        // convert from StreamSets model to JSON
        val writer = StringWriter()
        JsonRecordWriterImpl(writer, Mode.MULTIPLE_OBJECTS).apply {
            write(record)
            flush()
            close()
        }

        // output as pretty JSON to the json field used for file output
        val json = ObjectMapper().let { mapper ->
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(writer.toString()))
        }
        record.set(systemFieldForJsonOutput, Field.create(json)) // TODO: make this field configurable

        // put back the temporary input XML field
        if (tempXmlInput != null) {
            record.set(inputFieldNameFixed, inputField)
        }

        // set downstream operation to insert (1) or upsert (4)
        record.header.setAttribute("sdc.operation.type", "1")
        */

        return record
    }

    override fun process(record: Record, batchMaker: SingleLaneBatchMaker) {
        fixXmlToBetterJson(record)
        batchMaker.addRecord(record)
    }


}