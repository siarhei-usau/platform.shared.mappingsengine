package com.ebsco.platform.shared.mappingsengine.cli;

import com.ebsco.platform.shared.mappingsengine.config.MappingsEngineJsonConfig;
import com.ebsco.platform.shared.mappingsengine.config.Xml2JsonConfig;
import com.ebsco.platform.shared.mappingsengine.core.MappingsEngine;
import com.ebsco.platform.shared.mappingsengine.xml.XmlToRecordParser;
import com.ebsco.platform.shared.mappingsengine.xml.XmlToRecordParserConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.SingleCommand;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;
import com.github.rvesse.airline.parser.ParseResult;
import com.github.rvesse.airline.parser.errors.ParseOptionMissingException;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import lombok.*;

import javax.inject.Inject;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class MappingEngineCliApp {

    @Getter
    private File configFile;
    @Getter
    private File xmlInputFile;
    @Getter
    private File jsonOutputFile;

    public static void main(String[] args) {

        SingleCommand<Process> command = SingleCommand.singleCommand(Process.class);
        ParseResult<Process> parseResult = null;
        try {
            parseResult = command.parseWithResult(args);
        } catch (ParseOptionMissingException e) {
            System.out.println(e.getMessage());
            System.exit(-1);
        }

        Process cmd = parseResult.getCommand();
        if (cmd.helpOption.help || cmd.helpOption.showHelpIfErrors(parseResult)) {
            System.exit(-1);
        }
        cmd.run();
    }

    @Command(name = "mapping-engine-cli", description = "process XML files outputting the JSON equivalent")
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Process implements Runnable {

        @Inject
        private HelpOption<Process> helpOption;

        @Option(name = {"--xml", "-x"}, title = "input-XML", description = "input XML filename")
        @Required
        private String xmlInputFileName;

        @Option(name = {"--config", "-c"}, title = "configuration", description = "configuration filename for mappings instructions")
        @Required
        private String configFileName;

        @Option(name = {"--output", "-o"}, title = "output-JSON", description = "output JSON filename (optional)")
        private String jsonOutputFileName = "";

        @Override
        public void run() {
            if (!new File(xmlInputFileName).exists()) {
                System.out.println("XML input filename does not exist: $inputXmlFilename");
                helpOption.help = true;
                helpOption.showHelpIfRequested();
                System.exit(-1);
            }
            if (!new File(configFileName).exists()) {
                System.out.println("Config filename does not exist: $configFilename");
                helpOption.help = true;
                helpOption.showHelpIfRequested();
                System.exit(-1);
            }
            File jsonOutputFile = jsonOutputFileName.isEmpty() ? null : new File(jsonOutputFileName);
            MappingEngineCliApp app = new MappingEngineCliApp(new File(configFileName), new File(xmlInputFileName), jsonOutputFile);
            app.execute();
        }
    }

    @SneakyThrows
    private void execute() {
        System.out.println("Mapping-Engine TEST Tool");
        System.out.println("processing XML");

        MappingsEngineJsonConfig config = MappingsEngineJsonConfig.fromJson(new FileInputStream(configFile)); // TODO: close it
        Xml2JsonConfig xml2JsonConfig = config.getConfiguration().getXml2json();

        JsonProvider jsonProvider = new JacksonJsonProvider();
        XmlToRecordParserConfig parserConfig = XmlToRecordParserConfig.builder()
                .preserveNestedTextElements_ByXPath(xml2JsonConfig.getEmbedLiteralXmlAtPaths())
                .preserveNestedTextElements_AutoDetect(xml2JsonConfig.isAutoDetectMixedContent())
                .preserveNestedTextElements_UnhandledResultInError(xml2JsonConfig.isUnhandledMixedContentIsError())
                .forceSingleValueNodes_ByXPath(xml2JsonConfig.getForceSingleValueElementAtPaths())
                .forceElevateTextNode_ByXPath(xml2JsonConfig.getForceElevateTextNodesAtPaths())
                .forceElevateTextNodesAreSingleValued(xml2JsonConfig.isForceElevateTextNodesAsSingleValue())
                .textNodeName(xml2JsonConfig.getTextNodeName())
                .attributeNodePrefix(xml2JsonConfig.getAttributeNodePrefix())
                .attributePrefixesToKeep(xml2JsonConfig.getPreserveAttributePrefixes())
                .jsonProvider(jsonProvider)
                .build();

        XmlToRecordParser parser = new XmlToRecordParser(parserConfig);

        MappingsEngine mappings = MappingsEngine.builder()
                .transforms(config.getTransforms())
                .jsonProvider(jsonProvider)
                .build();

        try (InputStream is = new FileInputStream(xmlInputFile)) {
            XmlToRecordParser.Result parsed = parser.parse(is);
            Map<String, Map<String, Object>> jsonObject = new HashMap<>();
            jsonObject.put(parsed.getName(), (Map<String, Object>) parsed.getJsonNode());

            mappings.processDocument(jsonObject);
            System.out.println();

            String prettyJson = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
            System.out.println(prettyJson);

            if (jsonOutputFile == null) {
               return;
            }
            try(OutputStream os = new FileOutputStream(jsonOutputFile)) {
                os.write(prettyJson.getBytes());
            }

            System.out.println();
            System.out.println("Done");
        }

    }
}
