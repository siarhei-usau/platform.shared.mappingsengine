package com.ebsco.platform.shared.mappingsengine.cli

import com.ebsco.platform.shared.mappingsengine.core.MappingsEngine
import com.ebsco.platform.shared.mappingsengine.core.MappingsEngineConfig
import com.ebsco.platform.shared.mappingsengine.xml.XmlToRecordParser
import com.ebsco.platform.shared.mappingsengine.xml.XmlToRecordParserConfig
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.rvesse.airline.HelpOption
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import java.io.File
import javax.inject.Inject
import com.github.rvesse.airline.SingleCommand;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required
import com.github.rvesse.airline.parser.errors.ParseOptionMissingException

class MappingEngineCliApp(val configFile: File, val xmlInputFile: File, val jsonOutputFile: File?) {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            // TODO: replace airline, it isn't working correctly to output help and instead is throwing exceptions

            val result = SingleCommand.singleCommand(Process::class.java).let {
                try {
                    it.parseWithResult(*args)
                }
                catch (pme: ParseOptionMissingException) {
                    println(pme.message)
                    System.exit(-1)
                    null
                }
            }!!
            val cmd = result.command
            if (cmd.helpOption.help || cmd.helpOption.showHelpIfErrors(result)) {
                System.exit(-1)
            }

            // TODO: help and error handling is not working so well here.

            cmd.run()
        }

    }

    @Command(name = "mapping-engine-cli", description = "process XML files outputting the JSON equivalent")
    class Process : Runnable {
        @Inject
        lateinit var helpOption: HelpOption<Process>

        @Option(name = arrayOf("--xml", "-x"), title = "input-XML", description = "input XML filename")
        @Required
        lateinit var inputXmlFilename: String

        @Option(name = arrayOf("--config", "-c"),title = "configuration", description = "configuration filename for mappings instructions")
        @Required
        lateinit var configFilename: String

        @Option(name = arrayOf("--output", "-o"), title = "output-JSON", description = "output JSON filename (optional)")
        var outputJsonFilename: String = ""

        override fun run() {
            if (!File(inputXmlFilename).exists()) {
                println("XML input filename does not exist: $inputXmlFilename")
                helpOption.help = true
                helpOption.showHelpIfRequested()
                System.exit(-1)
            }
            if (!File(configFilename).exists()) {
                println("Config filename does not exist: $configFilename")
                helpOption.help = true
                helpOption.showHelpIfRequested()
                System.exit(-1)
            }
            MappingEngineCliApp(File(configFilename), File(inputXmlFilename), outputJsonFilename?.let { File(it) }).execute()
        }
    }

    fun execute() {
        println("Mapping-Engine TEST Tool")
        println("processing XML")

        val parser = XmlToRecordParser(XmlToRecordParserConfig.DEFAULTS_WITH_ERS_TEMP_PATHS.copy(
                preserveNestedTextElements_AutoDetect = false,
                preserveNestedTextElements_UnhandledResultInError = true,
                jsonProvider = JacksonJsonProvider(),
                textNodeName = "value",
                attributeNodePrefix = ""
        ))

        val mappings = MappingsEngine(MappingsEngineConfig.DEFAULTS.copy(jsonProvider = parser.config.jsonProvider))

        val (rootNodeName, innerObject) = xmlInputFile.inputStream().use { parser.parse(it) }
        @Suppress("UNCHECKED_CAST")
        val jsonObject = mapOf(rootNodeName to innerObject as Map<String, Any>)

        mappings.processDocument(jsonObject)

        println()

        val prettyJson = ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject)
        println(prettyJson)
        if (jsonOutputFile != null) {
            jsonOutputFile.outputStream().use {
                it.write(prettyJson.toByteArray())
            }
        }

        println()
        println("done.")
    }
}