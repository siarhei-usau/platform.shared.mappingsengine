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
import kotlin.text.Charsets;
import lombok.NonNull;
import lombok.val;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;

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

    private void println(final String msg) {
        System.out.println(msg);
    }

    private void println() {
        System.out.println();
    }

    public void run() throws IOException {
        println("Mapping-Engine TEST Tool");
        println("processing XML");

        MappingsEngineJsonConfig cfgFile = null;
        try (InputStream cfgInputStream = new FileInputStream(configFile)) {
            cfgFile = MappingsEngineJsonConfig.fromJson(cfgInputStream);
        }

        val xml2jsonCfg = cfgFile.getConfiguration().getXml2json();
        val parser = new XmlToRecordParser(new XmlToRecordParserConfig(
                xml2jsonCfg.getEmbedLiteralXmlAtPaths(),
                xml2jsonCfg.isAutoDetectMixedContent(),
                xml2jsonCfg.isUnhandledMixedContentIsError(),
                xml2jsonCfg.getForceSingleValueElementAtPaths(),
                xml2jsonCfg.getForceElevateTextNodesAtPaths(),
                xml2jsonCfg.isForceElevateTextNodesAsSingleValue(),
                new JacksonJsonProvider(),
                xml2jsonCfg.getTextNodeName(),
                xml2jsonCfg.getAttributeNodePrefix(),
                new HashSet<>(xml2jsonCfg.getPreserveAttributePrefixes())
        ));

        val mappings = new MappingsEngine(cfgFile.getTransforms(),
                DefaultTransformers.TRANFORMERS,
                parser.getConfig().getJsonProvider());

        try (final InputStream xmlInputStream = new FileInputStream(inputXmlFile)) {
            val rootWithDocument = parser.parse(xmlInputStream);
            val rootNodeName = rootWithDocument.getFirst();
            val innerObject = rootWithDocument.getSecond();

            val jsonObject = new HashMap<String, Object>();
            jsonObject.put(rootNodeName, innerObject);

            mappings.processDocument(jsonObject);

            println();

            val prettyJson = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
            println(prettyJson);
            if (outputJsonFile != null) {
                try (OutputStream jsonOutputStream = new FileOutputStream(outputJsonFile)) {
                    jsonOutputStream.write(prettyJson.getBytes(Charsets.UTF_8));
                }
            }

            println();
            println("done.");
        }
    }
}

