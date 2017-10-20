package com.ebsco.platform.shared.mappingsengine.xml

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import org.junit.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals

class TestXmlReader {
    private val testXML = """
            <thing xmlns:h="http://www.w3.org/TR/html4/">
                <identifier type="doi">someIdent</identifier>
                <container wassup="true">
                  <insideObject>textInside</insideObject>
                  <otherObject>otherText</otherObject>
                </container>
                <listy type="multi">
                  <item type="animal">monkey</item>
                  <item type="animal">dog</item>
                  <item type="animal">cat</item>
                </listy>
                <mixed1>Something<p>with</p>other</mixed1>
                <mixed2><p>Something <bold>is</bold> nested</p></mixed2>
                <mixed3>
                  <p>
                     this was what we had<br/>
                     and then they <emphasis>said</emphasis> no!
                  </p>
                </mixed3>
            </thing>
            """

    private fun printTree(tree: JsonNode) {
        println()
        println("Pretty JSON")
        val prettyJson = ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(tree)
        println(prettyJson)
    }

    fun XmlToRecordParser.parseXmlToTree(xml: String): JsonNode {
        val (rootNode, untypedTree) = parse(ByteArrayInputStream(testXML.toByteArray()))
        val JSON = ObjectMapper()
        @Suppress("UNCHECKED_CAST")
        val tree: JsonNode = JSON.valueToTree(mapOf(rootNode to untypedTree as Map<String, Any>))
        return tree
    }

    @Test
    fun `parsing valid XML with valid settings results in valid JSON tree`() {
        val parser = XmlToRecordParser(XmlToRecordParserConfig(
                preserveNestedTextElements_ByXPath = listOf(
                        """//thing/mixed1""",
                        """//thing/mixed2""",
                        """//thing/mixed3"""),
                preserveNestedTextElements_AutoDetect = false,
                jsonProvider = JacksonJsonProvider()
        ))
        val tree = parser.parseXmlToTree(testXML)

        // for debugging, output tree text
        printTree(tree)

        val identifier = tree.path("thing").path("identifier").single()
        assertEquals("doi", identifier.path("@type").asText())
        assertEquals("someIdent", identifier.path("#text").asText())

        val container = tree.path("thing").path("container").single()
        assertEquals("textInside", container.path("insideObject").path("#text").asText())
        assertEquals("otherText", container.path("otherObject").path("#text").asText())

        val listy = tree.path("thing").path("listy").single()
        assertEquals(listOf("animal", "animal", "animal"), listy.path("item").map { it.path("@type").asText() })
        assertEquals(listOf("monkey", "dog", "cat"), listy.path("item").map { it.path("#text").asText() })

        assertEquals("""Something<p>with</p>other""", tree.path("thing").path("mixed1").path("#text").asText())
        assertEquals("""<p>Something <bold>is</bold> nested</p>""", tree.path("thing").path("mixed2").path("#text").asText())
        assertEquals("""
            |<p>
            |   this was what we had<br/>
            |   and then they <emphasis>said</emphasis> no!
            |</p>
            """.trimMargin("|"), tree.path("thing").path("mixed3").path("#text").asText())

    }

    @Test
    fun `test alternative attribute prefixes and text node names`() {
        val parser = XmlToRecordParser(XmlToRecordParserConfig(
                preserveNestedTextElements_ByXPath = listOf(),
                preserveNestedTextElements_AutoDetect = true,
                jsonProvider = JacksonJsonProvider(),
                textNodeName = "value",
                attributeNodePrefix = ""
        ))
        val tree = parser.parseXmlToTree(testXML)

        // for debugging, output tree text
        printTree(tree)

        val identifier = tree.path("thing").path("identifier").single()
        assertEquals("doi", identifier.path("type").asText())
        assertEquals("someIdent", identifier.path("value").asText())

        val container = tree.path("thing").path("container").single()
        assertEquals("textInside", container.path("insideObject").path("value").asText())
        assertEquals("otherText", container.path("otherObject").path("value").asText())

        val listy = tree.path("thing").path("listy").single()
        assertEquals(listOf("animal", "animal", "animal"), listy.path("item").map { it.path("type").asText() })
        assertEquals(listOf("monkey", "dog", "cat"), listy.path("item").map { it.path("value").asText() })

        assertEquals("""Something<p>with</p>other""", tree.path("thing").path("mixed1").path("value").asText())

    }

    @Test(expected = IllegalStateException::class)
    fun `ensure that nested XML breaks if not configured correctly, due to missing XPath`() {
        val parser = XmlToRecordParser(XmlToRecordParserConfig(
                preserveNestedTextElements_ByXPath = listOf(
                        """//thing/mixed1""",
                        // missing //thing/mixed2 will cause failure
                        """//thing/mixed3"""),
                preserveNestedTextElements_AutoDetect = false,
                jsonProvider = JacksonJsonProvider()
        ))
        parser.parseXmlToTree(testXML)
    }

    @Test
    fun `nested XML elements can work even without XPath, if autodetect is on`() {
        val parser = XmlToRecordParser(XmlToRecordParserConfig(
                preserveNestedTextElements_ByXPath = emptyList(),
                preserveNestedTextElements_AutoDetect = true,   // <--- focus of this test
                jsonProvider = JacksonJsonProvider()
        ))
        val tree = parser.parseXmlToTree(testXML)

        // for debugging, output tree text
        printTree(tree)

        /*
        {
           "thing" : {
                ...
                "mixed1" : {
                  "#text" : "Something<p>with</p>other"
                },
                "mixed2" : [ {
                  "p" : {
                    "#text" : "Something <bold>is</bold> nested"
                  }
                } ],
                ...
         */
        assertEquals("""Something<p>with</p>other""", tree.path("thing").path("mixed1").path("#text").asText())
        assertEquals("""Something <bold>is</bold> nested""", tree.path("thing").path("mixed2").first().path("p").path("#text").asText())
    }

    @Test
    fun `force elevating a #text field should make it appear in the parent object array`() {
        val parser = XmlToRecordParser(XmlToRecordParserConfig(
                preserveNestedTextElements_ByXPath = emptyList(),
                preserveNestedTextElements_AutoDetect = true,
                forceElevateTextNode_ByXPath = listOf(
                        """//thing/container[*]/insideObject""",   // <--- focus of this test
                        """//thing/container[*]/otherObject"""),   // <--- focus of this test
                jsonProvider = JacksonJsonProvider()
        ))
        val tree = parser.parseXmlToTree(testXML)

        // for debugging, output tree text
        printTree(tree)

        /*
        {
           "thing" : {
                ...
                "container" : [ {
                  "@wassup" : "true",
                  "insideObject" : [ "textInside" ],
                  "otherObject" : [ "otherText" ]
                } ],
                ...
         */

        val container = tree.path("thing").path("container").single()
        assertEquals("textInside", container.path("insideObject").single().asText())
        assertEquals("otherText", container.path("otherObject").single().asText())

    }

    @Test
    fun `force elevating a #text field should make it appear in the parent object when single value mode enabled`() {
        val parser = XmlToRecordParser(XmlToRecordParserConfig(
                preserveNestedTextElements_ByXPath = emptyList(),
                preserveNestedTextElements_AutoDetect = true,
                forceElevateTextNode_ByXPath = listOf(
                        """//thing/container[*]/insideObject""",
                        """//thing/container[*]/otherObject"""),
                forceElevateTextNodesAreSingleValued = true,   // <--- focus of this test
                jsonProvider = JacksonJsonProvider()
        ))
        val tree = parser.parseXmlToTree(testXML)

        // for debugging, output tree text
        printTree(tree)

        /*
        {
           "thing" : {
                ...
                "container" : [ {
                  "@wassup" : "true",
                  "insideObject" : "textInside",
                  "otherObject" : "otherText"
                } ],
                ...
         */

        val container = tree.path("thing").path("container").single()
        assertEquals("textInside", container.path("insideObject").asText())
        assertEquals("otherText", container.path("otherObject").asText())

    }

    @Test
    fun `force a value to single value should avoid the JSON array`() {
        val parser = XmlToRecordParser(XmlToRecordParserConfig(
                preserveNestedTextElements_ByXPath = emptyList(),
                preserveNestedTextElements_AutoDetect = true,
                forceElevateTextNode_ByXPath = listOf(
                        """//thing/container[*]/insideObject""",
                        """//thing/container[*]/otherObject"""),
                forceSingleValueNodes_ByXPath = listOf(
                        """//thing/container[*]/otherObject"""  // <--- focus of this test
                ),
                jsonProvider = JacksonJsonProvider()
        ))
        val tree = parser.parseXmlToTree(testXML)

        // for debugging, output tree text
        printTree(tree)

        /*
        {
           "thing" : {
                ...
                "container" : [ {
                  "@wassup" : "true",
                  "insideObject" : [ "textInside" ],
                  "otherObject" : "otherText"
                } ],
                ...
         */

        val container = tree.path("thing").path("container").single()
        assertEquals("textInside", container.path("insideObject").single().asText())
        assertEquals("otherText", container.path("otherObject").asText()) // this one is popped up a level

    }
}