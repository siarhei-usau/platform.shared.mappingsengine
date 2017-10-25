package com.ebsco.entarch.mappings.streamsets

import com.ebsco.platform.shared.mappingsengine.config.MappingsEngineJsonConfig
import com.ebsco.platform.shared.mappingsengine.core.DefaultTransformers
import com.ebsco.platform.shared.mappingsengine.core.MappingsEngine
import com.ebsco.platform.shared.mappingsengine.xml.XmlToRecordParser
import com.ebsco.platform.shared.mappingsengine.xml.XmlToRecordParserConfig
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.streamsets.pipeline.api.*
import com.streamsets.pipeline.api.base.OnRecordErrorException
import com.streamsets.pipeline.api.base.SingleLaneRecordProcessor
import org.slf4j.LoggerFactory
import java.io.InputStream

@StageDef(version = 1, label = "XML2JSON Canonical Processor", description = "", icon = "default.png", onlineHelpRefUrl = "")
@ConfigGroups(Groups::class)
@GenerateResourceBundle
class XmlToJsonCanonicalProcessor : SingleLaneRecordProcessor() {
    val LOG = LoggerFactory.getLogger(XmlToJsonCanonicalProcessor::class.java)
    @ConfigDef(required = true, type = ConfigDef.Type.TEXT, defaultValue = emptyConfigJson,
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

    @ConfigDef(required = true, type = ConfigDef.Type.MODEL,
            defaultValue = """/json""",
            label = "JSON output Field", displayPosition = 8, group = MAPPINGS_GROUP,
            description = "Output field to place the text version of the resulting JSON")
    @FieldSelectorModel(singleValued = true)
    @JvmField
    var outJsonField: String? = null


    private val cfgJson by lazy {
        MappingsEngineJsonConfig.fromJson(mappingInstructions!!)
    }

    private val xml2jsonCfg by lazy { cfgJson.configuration.xml2json }

    private val parser by lazy {
        XmlToRecordParser(XmlToRecordParserConfig(
                preserveNestedTextElements_ByXPath = xml2jsonCfg.embedLiteralXmlAtPaths,
                preserveNestedTextElements_AutoDetect = xml2jsonCfg.isAutoDetectMixedContent,
                preserveNestedTextElements_UnhandledResultInError = xml2jsonCfg.isUnhandledMixedContentIsError,
                forceSingleValueNodes_ByXPath = xml2jsonCfg.forceSingleValueElementAtPaths,
                forceElevateTextNode_ByXPath = xml2jsonCfg.forceElevateTextNodesAtPaths,
                forceElevateTextNodesAreSingleValued = xml2jsonCfg.isForceElevateTextNodesAsSingleValue,
                textNodeName = xml2jsonCfg.textNodeName,
                attributeNodePrefix = xml2jsonCfg.attributeNodePrefix,
                attributePrefixesToKeep = xml2jsonCfg.preserveAttributePrefixes.toSet(),
                jsonProvider = JacksonJsonProvider()
        ))
    }

    private val mappings by lazy { MappingsEngine(cfgJson.transforms, DefaultTransformers.TRANFORMERS, parser.config.jsonProvider) }

    private val jsonMapper = jacksonObjectMapper()

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
        try {
            val inputFieldNameFixed = inputFieldName.takeIf { it.isNotBlank() }?.mustStartWith("/")
                    ?: "/fileRef"
            val inputField = record.get(inputFieldNameFixed)

            val inputStream = if (inputField.type == Field.Type.STRING) {
                inputField.valueAsString.toByteArray().inputStream()
            } else {
                inputField.valueAsFileRef.createInputStream(context, InputStream::class.java)
            }

            val (xmlField, xmlData) = inputStream.use { parser.parse(it) }

            @Suppress("UNCHECKED_CAST")
            val jsonObject = mapOf(xmlField to xmlData as Map<String, Any>)

            mappings.processDocument(jsonObject)

            // we go direct to a JSON field, which can be parsed by streamsets if desired to be operated upon further.

            val json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject)

            record.set(outJsonField ?: "/json", Field.create(json))

            return record
        } catch (ex: Throwable) {
            throw OnRecordErrorException(record, Errors.EBSCO_RECORD_ERROR, ex.message)
        }
    }

    override fun process(record: Record, batchMaker: SingleLaneBatchMaker) {
        fixXmlToBetterJson(record)
        batchMaker.addRecord(record)
    }


}