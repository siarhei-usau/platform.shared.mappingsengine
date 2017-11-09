package com.ebsco.entarch.mappings.streamsets;

import com.ebsco.platform.shared.mappingsengine.config.MappingsEngineJsonConfig;
import com.ebsco.platform.shared.mappingsengine.config.Xml2JsonConfig;
import com.ebsco.platform.shared.mappingsengine.core.MappingsEngine;
import com.ebsco.platform.shared.mappingsengine.xml.XmlToRecordParser;
import com.ebsco.platform.shared.mappingsengine.xml.XmlToRecordParserConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.streamsets.pipeline.api.*;
import com.streamsets.pipeline.api.base.OnRecordErrorException;
import com.streamsets.pipeline.api.base.SingleLaneRecordProcessor;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@StageDef(version = 1, label = "XML2JSON Canonical Processor", icon = "default.png", onlineHelpRefUrl = "")
@ConfigGroups(Groups.class)
@GenerateResourceBundle
public class XmlToJsonCanonicalProcessor extends SingleLaneRecordProcessor {

    private static final String EMPTY_CONFIG = "{\n" +
            "  \"metadata\": {\n" +
            "    \"id\": \"empty\",\n" +
            "    \"version\": \"1.0.0\",\n" +
            "    \"primaryKey\": \"$.id\"\n" +
            "  },\n" +
            "  \"transforms\": [\n" +
            "  ],\n" +
            "  \"configuration\": {\n" +
            "    \"xml2json\": {\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @ConfigDef(required = true, type = ConfigDef.Type.TEXT, defaultValue = EMPTY_CONFIG,
            label = "Mappings JSON", displayPosition = 10, group = "MAPPINGS",
            mode = ConfigDef.Mode.JSON,
            lines = 15,
            description = "Mappings instruction set as JSON")
    public String mappingInstructions = null;

    @ConfigDef(required = true, type = ConfigDef.Type.MODEL,
            defaultValue = "/fileRef",
            label = "Input Field", displayPosition = 6, group = "MAPPINGS",
            description = "Input Field, can be fileRef or string field containing content")
    @FieldSelectorModel(singleValued = true)
    public String rawXmlField = null; // TODO: slightly misnamed but breaks existing flows to rename

    @ConfigDef(required = true, type = ConfigDef.Type.MODEL,
               defaultValue = "XML",
               label = "Input Type", displayPosition = 5, group = "MAPPINGS",
               description = "Input format (XML, JSON, ...)")
    @ValueChooserModel(InputTypesChooser.class)
    public InputTypes rawInputType = InputTypes.XML;

    @ConfigDef(required = true, type = ConfigDef.Type.MODEL,
            defaultValue = "/json",
            label = "JSON output Field", displayPosition = 8, group = "MAPPINGS",
            description = "Output field to place the text version of the resulting JSON")
    @FieldSelectorModel(singleValued = true)
    public String outJsonField = null;

    @Getter(lazy = true)
    private final MappingsEngineJsonConfig cfgJson = MappingsEngineJsonConfig.fromJson(mappingInstructions);

    @Getter(lazy = true)
    private final Xml2JsonConfig xml2JsonConfig = getCfgJson().getConfiguration().getXml2json();

    @Getter(lazy = true)
    private final XmlToRecordParser parser = buildParser();

    @Getter(lazy = true)
    private final MappingsEngine mappings = MappingsEngine.builder()
            .transforms(getCfgJson().getTransforms())
            .jsonProvider(getParser().getJson())
            .build();

    private final ObjectMapper jsonMapper = new ObjectMapper();


    @Override
    public List<ConfigIssue> init(Info info, Processor.Context context) {
        List<ConfigIssue> issues = super.init(info, context);
        if (mappingInstructions.equals("invalidValue")) {
            issues.add(context.createConfigIssue(Groups.MAPPINGS.getLabel(), "mappingInstructions",
                    Errors.EBSCO_INVALID_CONFIG, "mappings mappingInstructions JSON is invalid")); // TODO: add errors from mapping system
        }
        if (rawXmlField.equals("invalidValue")) {
            issues.add(context.createConfigIssue(Groups.MAPPINGS.getLabel(), "rawXmlField",
                    Errors.EBSCO_INVALID_CONFIG, "field name for XML input is invalid")); // TODO: add errors from mapping system
        }
        return issues;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    protected void process(Record record, SingleLaneBatchMaker batchMaker) throws StageException {
        applyMappings(record, rawXmlField);
        batchMaker.addRecord(record);
    }

    private void applyMappings(Record record, String inputFieldName) throws OnRecordErrorException {
        try {
            String inputFieldNameFixed = fixInputFieldName(inputFieldName);
            Field inputField = record.get(inputFieldNameFixed);
            try(InputStream is = getInputStream(inputField)) {
                final Map<String, Map<String, Object>> jsonObject;
                if (rawInputType == InputTypes.XML) {
                    XmlToRecordParser.Result parsed = getParser().parse(is);
                    jsonObject = new HashMap<>();
                    jsonObject.put(parsed.getName(), (Map<String, Object>) parsed.getJsonNode());
                } else if (rawInputType == InputTypes.JSON) {
                    jsonObject = jsonMapper.readValue(is, Map.class);
                } else {
                    throw new IllegalArgumentException("Input Type " + rawInputType.name() + " is not supported.");
                }
                getMappings().processDocument(jsonObject);

                String prettyJson = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
                Field field = Field.create(prettyJson);
                if (outJsonField == null) {
                    record.set("/json", field);
                } else {
                    record.set(outJsonField, field);
                }
            }
        } catch (Exception e) {
            throw new OnRecordErrorException(record, Errors.EBSCO_RECORD_ERROR, e.getMessage());
        }
    }

    private InputStream getInputStream(Field inputField) throws IOException {
        InputStream inputStream;
        if (inputField.getType() == Field.Type.STRING) {
            inputStream =  new ByteArrayInputStream(inputField.getValueAsString().getBytes());
        } else {
            inputStream = inputField.getValueAsFileRef().createInputStream(getContext(), InputStream.class);
        }
        return inputStream;
    }

    private String fixInputFieldName(String inputFieldName) {
        if (inputFieldName != null && !inputFieldName.trim().isEmpty() && inputFieldName.startsWith("/")) {
            return inputFieldName;
        } else {
            return "/fileRef";
        }
    }

    private XmlToRecordParser buildParser() {
        Xml2JsonConfig xml2JsonConfig = getXml2JsonConfig();
        XmlToRecordParserConfig xmlToRecordParserConfig = XmlToRecordParserConfig.builder()
                .preserveNestedTextElements_ByXPath(xml2JsonConfig.getEmbedLiteralXmlAtPaths())
                .preserveNestedTextElements_AutoDetect(xml2JsonConfig.isAutoDetectMixedContent())
                .preserveNestedTextElements_UnhandledResultInError(xml2JsonConfig.isUnhandledMixedContentIsError())
                .forceSingleValueNodes_ByXPath(xml2JsonConfig.getForceSingleValueElementAtPaths())
                .forceElevateTextNode_ByXPath(xml2JsonConfig.getForceElevateTextNodesAtPaths())
                .forceElevateTextNodesAreSingleValued(xml2JsonConfig.isForceElevateTextNodesAsSingleValue())
                .textNodeName(xml2JsonConfig.getTextNodeName())
                .attributeNodePrefix(xml2JsonConfig.getAttributeNodePrefix())
                .attributePrefixesToKeep(xml2JsonConfig.getPreserveAttributePrefixes())
                .jsonProvider(new JacksonJsonProvider())
                .build();
        return new XmlToRecordParser(xmlToRecordParserConfig);
    }
}
