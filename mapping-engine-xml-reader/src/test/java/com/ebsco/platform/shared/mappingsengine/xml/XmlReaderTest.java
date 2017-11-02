package com.ebsco.platform.shared.mappingsengine.xml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import lombok.val;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

public class XmlReaderTest {
    private static final String textXML = "<thing xmlns:h=\"http://www.w3.org/TR/html4/\">\n" +
            "                <identifier type=\"doi\">someIdent</identifier>\n" +
            "                <container wassup=\"true\">\n" +
            "                  <insideObject>textInside</insideObject>\n" +
            "                  <otherObject>otherText</otherObject>\n" +
            "                </container>\n" +
            "                <listy type=\"multi\">\n" +
            "                  <item type=\"animal\">monkey</item>\n" +
            "                  <item type=\"animal\">dog</item>\n" +
            "                  <item type=\"animal\">cat</item>\n" +
            "                </listy>\n" +
            "                <mixed1>Something<p>with</p>other</mixed1>\n" +
            "                <mixed2><p>Something <bold>is</bold> nested</p></mixed2>\n" +
            "                <mixed3>\n" +
            "                  <p>\n" +
            "                     this was what we had<br/>\n" +
            "                     and then they <emphasis>said</emphasis> no!\n" +
            "                  </p>\n" +
            "                </mixed3>\n" +
            "            </thing>";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    private void printTree(JsonNode tree) throws Exception {
        System.out.println("Pretty JSON");
        String prettyJson = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
        System.out.println(prettyJson);
    }

    private JsonNode parseXmlToTree(XmlToRecordParser parser) {
        XmlToRecordParser.Result result = parser.parse(new ByteArrayInputStream(textXML.getBytes()));
        String rootNode = result.getName();
        Object untypedTree = result.getJsonNode();
        Map<String, Object> m = new HashMap<>();
        m.put(rootNode, untypedTree);
        JsonNode tree = OBJECT_MAPPER.valueToTree(m);
        return tree;
    }

    private <T> Stream<T> toStream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    @Test
    // parsing valid XML with valid settings results in valid JSON tree
    public void parsesWithValidSettings() throws Exception {
        XmlToRecordParserConfig config = XmlToRecordParserConfig.builder()
                .preserveNestedTextElements_ByXPath(asList(
                        "//thing/mixed1",
                        "//thing/mixed2",
                        "//thing/mixed3"))
                .preserveNestedTextElements_AutoDetect(false)
                .jsonProvider(new JacksonJsonProvider())
                .build();
        XmlToRecordParser parser = new XmlToRecordParser(config);
        JsonNode tree = parseXmlToTree(parser);

        // for debugging, output tree text
        printTree(tree);

        JsonNode identifier = tree.path("thing").path("identifier").get(0);
        assertEquals("doi", identifier.path("@type").asText());
        assertEquals("someIdent", identifier.path("#text").asText());

        JsonNode container = tree.path("thing").path("container").get(0);
        assertEquals("textInside", container.path("insideObject").path("#text").asText());
        assertEquals("otherText", container.path("otherObject").path("#text").asText());

        JsonNode listy = tree.path("thing").path("listy").get(0);
        JsonNode item = listy.path("item");
        assertEquals(asList("animal", "animal", "animal"), toStream(item).map(it -> it.path("@type").asText()).collect(toList()));
        assertEquals(asList("monkey", "dog", "cat"), toStream(item).map(it -> it.path("#text").asText()).collect(toList()));

        assertEquals("Something<p>with</p>other", tree.path("thing").path("mixed1").path("#text").asText());
        assertEquals("<p>Something <bold>is</bold> nested</p>", tree.path("thing").path("mixed2").path("#text").asText());

        String expected = "<p>this was what we had<br/>and then they <emphasis>said</emphasis> no!</p>".replaceAll("\\s", "");
        String actual = tree.path("thing").path("mixed3").path("#text").asText().replaceAll("\\s", "");
        assertEquals(expected, actual);
    }

    @Test
    //test alternative attribute prefixes and text node names
    public void alternativeAttributesWithPrefixesAndTextNodeNames() throws Exception {
        XmlToRecordParserConfig config = XmlToRecordParserConfig.builder()
                .preserveNestedTextElements_ByXPath(emptyList())
                .preserveNestedTextElements_AutoDetect(true)
                .jsonProvider(new JacksonJsonProvider())
                .textNodeName("value")
                .attributeNodePrefix("")
                .build();
        XmlToRecordParser parser = new XmlToRecordParser(config);
        JsonNode tree = parseXmlToTree(parser);

        // for debugging, output tree text
        printTree(tree);

        JsonNode identifier = tree.path("thing").path("identifier").get(0);
        assertEquals("doi", identifier.path("type").asText());
        assertEquals("someIdent", identifier.path("value").asText());

        JsonNode container = tree.path("thing").path("container").get(0);
        assertEquals("textInside", container.path("insideObject").path("value").asText());
        assertEquals("otherText", container.path("otherObject").path("value").asText());

        JsonNode listy = tree.path("thing").path("listy").get(0);
        JsonNode item = listy.path("item");
        assertEquals(asList("animal", "animal", "animal"), toStream(item).map(it -> it.path("type").asText()).collect(toList()));
        assertEquals(asList("monkey", "dog", "cat"), toStream(item).map(it -> it.path("value").asText()).collect(toList()));

        assertEquals("Something<p>with</p>other", tree.path("thing").path("mixed1").path("value").asText());
    }

    @Test(expected = IllegalStateException.class)
    // ensure that nested XML breaks if not configured correctly, due to missing XPath
    public void nestedXmlBreaksIfNotConfiguredCorrectly() {
        XmlToRecordParserConfig config = XmlToRecordParserConfig.builder()
                .preserveNestedTextElements_ByXPath(asList(
                        "//thing/mixed1",
                        // missing //thing/mixed2 will cause failure
                        "//thing/mixed3"))
                .preserveNestedTextElements_AutoDetect(false)
                .jsonProvider(new JacksonJsonProvider())
                .build();
        XmlToRecordParser parser = new XmlToRecordParser(config);
        parseXmlToTree(parser);
    }

    @Test
    // nested XML elements can work even without XPath, if autodetect is on
    public void nestedXmlElementsWorkWithoutXpathWithAutodetect() throws Exception {
        XmlToRecordParserConfig config = XmlToRecordParserConfig.builder()
                .preserveNestedTextElements_ByXPath(emptyList())
                .preserveNestedTextElements_AutoDetect(true)
                .jsonProvider(new JacksonJsonProvider())
                .build();
        XmlToRecordParser parser = new XmlToRecordParser(config);
        JsonNode tree = parseXmlToTree(parser);

        // for debugging, output tree text
        printTree(tree);

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
        assertEquals("Something<p>with</p>other", tree.path("thing").path("mixed1").path("#text").asText());
        assertEquals("Something <bold>is</bold> nested", tree.path("thing").path("mixed2").get(0).path("p").path("#text").asText());
    }

    @Test
    // force elevating a #text field should make it appear in the parent object array
    public void testElevatingField() throws Exception {
        XmlToRecordParserConfig config = XmlToRecordParserConfig.builder()
                .preserveNestedTextElements_ByXPath(emptyList())
                .preserveNestedTextElements_AutoDetect(true)
                .forceElevateTextNode_ByXPath(asList(
                        "//thing/container[*]/insideObject",
                        "//thing/container[*]/otherObject"))
                .jsonProvider(new JacksonJsonProvider())
                .build();
        XmlToRecordParser parser = new XmlToRecordParser(config);
        JsonNode tree = parseXmlToTree(parser);

        // for debugging, output tree text
        printTree(tree);

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

        JsonNode container = tree.path("thing").path("container").get(0);
        assertEquals("textInside", container.path("insideObject").get(0).asText());
        assertEquals("otherText", container.path("otherObject").get(0).asText());
    }

    @Test
    // force elevating a #text field should make it appear in the parent object when single value mode enabled
    public void testSingleValueMode() throws Exception {
        XmlToRecordParserConfig config = XmlToRecordParserConfig.builder()
                .preserveNestedTextElements_ByXPath(emptyList())
                .preserveNestedTextElements_AutoDetect(true)
                .forceElevateTextNode_ByXPath(asList(
                        "//thing/container[*]/insideObject",
                        "//thing/container[*]/otherObject"))
                .forceElevateTextNodesAreSingleValued(true)
                .jsonProvider(new JacksonJsonProvider())
                .build();
        XmlToRecordParser parser = new XmlToRecordParser(config);
        JsonNode tree = parseXmlToTree(parser);

        // for debugging, output tree text
        printTree(tree);

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

        val container = tree.path("thing").path("container").get(0);
        assertEquals("textInside", container.path("insideObject").asText());
        assertEquals("otherText", container.path("otherObject").asText());
    }

    @Test
    // force a value to single value should avoid the JSON array
    public void forceValueToSingleAvoidsJsonArray() throws Exception {
        val config = XmlToRecordParserConfig.builder()
                .preserveNestedTextElements_ByXPath(emptyList())
                .preserveNestedTextElements_AutoDetect(true)
                .forceElevateTextNode_ByXPath(asList(
                        "//thing/container[*]/insideObject",
                        "//thing/container[*]/otherObject"))
                .forceSingleValueNodes_ByXPath(singletonList(
                        "//thing/container[*]/otherObject"  // <--- focus of this test
                ))
                .jsonProvider(new JacksonJsonProvider())
                .build();
        XmlToRecordParser parser = new XmlToRecordParser(config);
        JsonNode tree = parseXmlToTree(parser);

        // for debugging, output tree text
        printTree(tree);

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

        val container = tree.path("thing").path("container").get(0);
        assertEquals("textInside", container.path("insideObject").get(0).asText());
        assertEquals("otherText", container.path("otherObject").asText()); // this one is popped up a level
    }
}
