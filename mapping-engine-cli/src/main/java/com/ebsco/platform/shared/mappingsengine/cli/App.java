package com.ebsco.platform.shared.mappingsengine.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.ebsco.platform.shared.mappingsengine.config.MappingsEngineJsonConfig;
import com.ebsco.platform.shared.mappingsengine.core.DefaultTransformers;
import com.ebsco.platform.shared.mappingsengine.core.MappingsEngine;
import com.ebsco.platform.shared.mappingsengine.xml.XmlToRecordParser;
import com.ebsco.platform.shared.mappingsengine.xml.XmlToRecordParserConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import lombok.NonNull;
import lombok.val;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class App {

    @NonNull
    private File configFile;

    @NonNull
    private File inputXmlFile;

    private File outputJsonFile;

    public App(final File configFile, final File inputXmlFile, final File outputJsonFile) {
        this.configFile = configFile;
        this.inputXmlFile = inputXmlFile;
        this.outputJsonFile = outputJsonFile;
    }

    public static void main(String[] args) {
        val parsedArgs = new CliArgs();

        try {
            val cmdline = JCommander.newBuilder()
                    .addObject(parsedArgs)
                    .build();

            cmdline.parse(args);

            val inputXmlFile = new File(parsedArgs.getXmlInputFileName());
            val configFile = new File(parsedArgs.getConfigFileName());

            if (!inputXmlFile.exists()) {
                printUsageAndExit("XML input filename does not exist: " + inputXmlFile.getAbsolutePath(), cmdline);
            }

            if (!configFile.exists()) {
                printUsageAndExit("Config file does not exist: " + configFile.getAbsolutePath(), cmdline);
            }

            File outputJsonFile = null;
            if (parsedArgs.getJsonInputFileName() != null) {
                outputJsonFile = new File(parsedArgs.getJsonInputFileName());
                if (!outputJsonFile.getParentFile().exists()) {
                    printUsageAndExit("JSON output file parent directory must already exist: " +
                            outputJsonFile.getParentFile().getAbsolutePath(), cmdline);
                }
            }


            try {
                new App(configFile, inputXmlFile, outputJsonFile).run();
            } catch (IOException ex) {
                System.err.println("Error: " + ex.getMessage());
                ex.printStackTrace();
                System.exit(-2);
            }
        } catch (ParameterException ex) {
            printUsageAndExit(ex.getMessage(), ex.getJCommander());
        }
    }

    private static void printUsageAndExit(final String errMessage, final JCommander cmdline) {
        System.err.println(errMessage);
        cmdline.usage();
        System.exit(-1);
    }

    public void run() throws IOException {
        MappingsEngineJsonConfig cfgFile = null;
        try (InputStream cfgInputStream = new FileInputStream(configFile)) {
            cfgFile = MappingsEngineJsonConfig.fromJson(cfgInputStream);
        }

        val xml2jsonCfg = cfgFile.getConfiguration().getXml2json();
        JsonProvider jsonProvider = new JacksonJsonProvider();
        val parser = new XmlToRecordParser(XmlToRecordParserConfig.builder()
                .preserveNestedTextElements_ByXPath(xml2jsonCfg.getEmbedLiteralXmlAtPaths())
                .preserveNestedTextElements_AutoDetect(xml2jsonCfg.isAutoDetectMixedContent())
                .preserveNestedTextElements_UnhandledResultInError(xml2jsonCfg.isUnhandledMixedContentIsError())
                .forceSingleValueNodes_ByXPath(xml2jsonCfg.getForceSingleValueElementAtPaths())
                .forceElevateTextNode_ByXPath(xml2jsonCfg.getForceElevateTextNodesAtPaths())
                .forceElevateTextNodesAreSingleValued(xml2jsonCfg.isForceElevateTextNodesAsSingleValue())
                .textNodeName(xml2jsonCfg.getTextNodeName())
                .attributeNodePrefix(xml2jsonCfg.getAttributeNodePrefix())
                .attributePrefixesToKeep(xml2jsonCfg.getPreserveAttributePrefixes())
                .jsonProvider(jsonProvider)
                .build());

        val mappings = MappingsEngine.builder().transforms(cfgFile.getTransforms()).transformerClasses(DefaultTransformers.TRANFORMERS).jsonProvider(parser.getConfig().getJsonProvider()).build();

        try (final InputStream xmlInputStream = new FileInputStream(inputXmlFile)) {
            String prettyJson = "";
            ObjectMapper mapper = new ObjectMapper();
            if (inputXmlFile.toURI().toString().endsWith(".xml")) {
                Map<String, Map<String, Object>> jsonObject = new HashMap<>();
                XmlToRecordParser.Result parsed = parser.parse(xmlInputStream);
                jsonObject.put(parsed.getName(), (Map<String, Object>) parsed.getJsonNode());
                mappings.processDocument(jsonObject);
                prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
            } else if (inputXmlFile.toURI().toString().endsWith(".json")) {
                val pureJsonInputObject = mapper.readTree(inputXmlFile);
                mappings.processDocument(pureJsonInputObject);
                prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pureJsonInputObject);
            }

            if (outputJsonFile != null) {
                try (OutputStream jsonOutputStream = new FileOutputStream(outputJsonFile)) {
                    jsonOutputStream.write(prettyJson.getBytes(Charset.forName("UTF-8")));
                }
            }
        }
    }
}

