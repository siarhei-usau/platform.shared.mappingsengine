package com.ebsco.entarch.mappings.streamsets

import _ss_com.fasterxml.jackson.databind.ObjectMapper
import _ss_com.streamsets.datacollector.json.JsonRecordWriterImpl
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
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

    val jsonPathConfig = Configuration.builder().options(Option.AS_PATH_LIST).build()
    val jsonPath = JsonPath.using(jsonPathConfig).registerMappingConfig(mappingInstructions)

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

        val parser = XmlToRecordParser(XmlToRecordParserConfig(
                preserveNestedTextElements_ByXPath = listOf(
                    "//book[*]/body[*]/book-part[*]/book-front[*]/sec",
                    "//book[*]/body[*]/book-part[*]/body[*]/sec"
                ),
                preserveNestedTextElements_AutoDetect = false,
                preserveNestedTextElements_UnhandledResultInError = true
        ))
        val (xmlField, xmlData) = inputStream.use { parser.parse(it) }

        record.set("/${xmlField}", xmlData)

        // XML attributes can be in the content as:
        //     /attr|xml:lang
        //     /attr|dtd-version
        //     /ns|xmlns:xlink
        //     /ns|xmlns:mml
        //     /ns|xmlns:xsi
        //     */attr|something
        // or as field attributes
        //     /xmlAttr:xml:lang
        //     /xmlAttr:dtd-version
        //     /xmlns:xlink
        //     /xmlns:mml
        //     /xmlns:xsi
        //     */xmlAttr:something
        //
        // TODO:  do we need to do anything with the non attribute XML information, name spaces for example
        // TODO:  what if attribute is on something that isn't a map, do we wrap it into a map?
        //

        fun relatedPaths() = record.escapedFieldPaths.toList().filter { it.startsWith("/$xmlField") }

        if (FEATURE_XML_ATTRIBUTES_TO_FIELDS) {
            // map attributes to a child a field
            relatedPaths().forEach { fieldName ->
                val currentField = record.get(fieldName) ?: throw IllegalStateException("field $fieldName does not exist but is in field list")
                (currentField.attributes ?: emptyMap()).forEach { key, value ->
                    if (key.startsWith("xmlAttr:")) {
                        if (currentField.type == Field.Type.MAP) {
                            val newAttrFieldName = "$fieldName/@${key.removePrefix("xmlAttr:")}"
                            record.set(newAttrFieldName, Field.create(value))
                        } else {
                            throw IllegalStateException("We have an attribute on a field $fieldName that is not a map")
                        }
                    }
                }

                val parsedPath = parseRecordPath(fieldName)
                val childFieldElement = parsedPath.last()
                if (childFieldElement.type == PathStepType.ELEMENT) {
                    val childFieldName = childFieldElement.name!!
                    if (childFieldName.startsWith("attr|")) {
                        val parentFieldPath = parsedPath.parentPath()
                        val newAttrFieldPath = parentFieldPath + PathStep(childFieldName.removePrefix("attr|"), PathStepType.ELEMENT)
                        val newAttrFieldName = newAttrFieldPath.pathAsString()
                        record.delete(fieldName)
                        record.set(newAttrFieldName, currentField)
                    }
                }

            }
        }

        if (FEATURE_ELEVATE_VALUE_TO_FIELD) {
            relatedPaths().forEach { fieldName ->
                //    fields that are a list with one element, that is a map with one key "value" elevate that to "#text" replacing the list
                //          ensuring to keep attributes that were on the list
                val parsedPath = parseRecordPath(fieldName)
                val childFieldElement = parsedPath.last()

                if (childFieldElement.type == PathStepType.ELEMENT && childFieldElement.name!! == "value") {
                    val valueContents = record.get(fieldName)
                    val parentFieldPath = parsedPath.parentPath()
                    val parentFieldName = parentFieldPath.pathAsString()
                    val parentField = record.get(parentFieldName) ?: throw IllegalStateException("Missing parent field for $fieldName of $parentFieldName")
                    if (parentField.type == Field.Type.MAP) {
                        if (parentField.valueAsMap.size == 1) {
                            // so our parentFieldname here is either /path/to/field or /path/to/field[N]
                            // and we expect it to be the latter
                            if (parentFieldPath.last().type == PathStepType.ARRAYINDEX) {
                                val parentFieldListPath = parentFieldPath.parentPath()
                                val parentFieldListName = parentFieldListPath.pathAsString()
                                val parentFieldList = record.get(parentFieldListName) ?: throw IllegalStateException("Missing parent list field for $fieldName of $parentFieldListName")
                                if (parentFieldList.type == Field.Type.LIST) {
                                    if (parentFieldList.valueAsList.size == 1) {
                                        // collect attributes from parentFieldList overridden by parentField
                                        valueContents.addAllAttributes(parentFieldList.attributes ?: emptyMap())
                                        valueContents.addAllAttributes(parentField.attributes ?: emptyMap())
                                        // move valueContents up to the parent position
                                        LOG.debug("Elevate $fieldName to $parentFieldListName")
                                        record.set(parentFieldListName, valueContents)
                                    } else {
                                        LOG.warn("Field $fieldName is a /value field but is unexpectedly not the only element in parent list")
                                    }
                                } else {
                                    throw IllegalStateException("Expected parent list field to be ARRAYINDEX but instead was ${parentFieldList.type}")
                                }
                            } else {
                                LOG.warn("Field $fieldName is a /value field but is unexpectedly in a parent map that is not in a list item [0]")
                            }
                        } else {
                            val hasAnyOtherRealFields = parentField.valueAsMap.any { it.key != "value" && !it.key.startsWith('@') }
                            if (hasAnyOtherRealFields) {
                                LOG.warn("Field $fieldName is a /value field but is unexpectedly not the only field in a map")
                            }
                        }
                    } else {
                        throw IllegalStateException("Expected parent field to be ELEMENT but instead was ${parentField.type}")
                    }
                }
            }
        }

        if (FEATURE_RENAME_VALUE_TO_TEXT || FEATURE_RENAME_TEXT_TO_VALUE) {
            val fromName = if (FEATURE_RENAME_VALUE_TO_TEXT) "value" else "#text"
            val toName = if (FEATURE_RENAME_VALUE_TO_TEXT) "#text" else "value"
            //    fields with name /value in maps should become /#text in the same map
            relatedPaths().forEach { fieldName ->
                val parsedPath = parseRecordPath(fieldName)
                val childFieldElement = parsedPath.last()

                if (childFieldElement.type == PathStepType.ELEMENT && childFieldElement.name!! == fromName) {
                    val valueContents = record.get(fieldName)
                    val parentFieldPath = parsedPath.parentPath()
                    val parentFieldName = parentFieldPath.pathAsString()
                    val parentField = record.get(parentFieldName) ?: throw IllegalStateException("Missing parent field for $fieldName of $parentFieldName")
                    if (parentField.type == Field.Type.MAP) {
                        val hasAnyOtherRealFields = parentField.valueAsMap.any { it.key != fromName && !it.key.startsWith('@') }
                        if (!hasAnyOtherRealFields) {
                            val newTextFieldPath = parentFieldPath + PathStep(toName, PathStepType.ELEMENT)
                            val newTextFieldName = newTextFieldPath.pathAsString()

                            record.delete(fieldName)
                            record.set(newTextFieldName, valueContents)
                        } else {
                            LOG.warn("Field $fieldName is a value or #text field but is unexpectedly not the only field in a map")
                        }
                    } else {
                        throw IllegalStateException("Expected parent field to be ELEMENT but instead was ${parentField.type}")
                    }
                }
            }
        }

        if (FEATURE_ATTRIBUTES_TO_NORMAL_NAME) {
            relatedPaths().forEach { fieldName ->
                val parsedPath = parseRecordPath(fieldName)
                val childFieldElement = parsedPath.last()
                if (childFieldElement.type == PathStepType.ELEMENT && childFieldElement.name!!.startsWith("@")) {
                    val currentField = record.get(fieldName) ?: throw IllegalStateException("field $fieldName does not exist but is in field list")
                    val parentFieldPath = parsedPath.parentPath()

                    val newAttrFieldPath = parentFieldPath + PathStep(childFieldElement.name.removePrefix("@"), PathStepType.ELEMENT)
                    val newAttrFieldName = newAttrFieldPath.pathAsString()

                    record.delete(fieldName)
                    record.set(newAttrFieldName, currentField)
                }
            }
        }

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

        // put back the temporary input XML field
        if (tempXmlInput != null) {
            record.set(inputFieldNameFixed, inputField)
        }

        // process Mappings as JsonPath manipulations
        val compiled = jsonPath.parse(writer.toString())
        compiled.processMappings()

        // set downstream operation to insert (1) or upsert (4)
        record.header.setAttribute("sdc.operation.type", "1")

        // output as pretty JSON to the json field used for file output
        val json = ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(compiled.json())
        record.set(systemFieldForJsonOutput, Field.create(json))

        return record
    }

    override fun process(record: Record, batchMaker: SingleLaneBatchMaker) {
        // LOG.info("Input record: {}", record.toString().replace('\n', ' '))

        // println("Record fields: ${record.escapedFieldPaths.toList()}")

        fixXmlToBetterJson(record)

        batchMaker.addRecord(record)
    }


}