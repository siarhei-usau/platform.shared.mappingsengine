package com.ebsco.platform.shared.mappingsengine.xml;

import com.jayway.jsonpath.spi.json.JsonProvider;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Value;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.ebsco.platform.shared.mappingsengine.xml.XmlUtils.*;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class XmlToRecordParser {
    @Getter
    final private XmlToRecordParserConfig config;
    @Getter
    final private JsonProvider json;

    public XmlToRecordParser(XmlToRecordParserConfig config) {
        this.config = config;
        this.json = config.getJsonProvider();
    }

    public XmlToRecordParser() {
        this.config = XmlToRecordParserConfig.DEFAULTS;
        this.json = config.getJsonProvider();
    }

    @SneakyThrows
    public Result parse(InputStream input) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setCoalescing(true);
        factory.setIgnoringComments(true);
        factory.setNamespaceAware(true);
        factory.setValidating(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));

        Document xmlDocument = builder.parse(input);
        cleanEmptyTextNodes(xmlDocument);

        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(new XmlNamespaceCache(xmlDocument, true));

        Set<Node> preserveNestedTextInNodes = xpathToNodes(config.getPreserveNestedTextElements_ByXPath(), xPath, xmlDocument);
        Set<Node> forceSingleValueNodes = xpathToNodes(config.getForceSingleValueNodes_ByXPath(), xPath, xmlDocument);
        Set<Node> forceElevateTextNodes = xpathToNodes(config.getForceElevateTextNode_ByXPath(), xPath, xmlDocument);

        return buildRootField(xmlDocument, preserveNestedTextInNodes, forceSingleValueNodes, forceElevateTextNodes);
    }

    private Result buildRootField(Document xmlDocument,
                                Set<Node> preserveNestedTextInNodes,
                                Set<Node> forceSingleValueNodes,
                                Set<Node> forceElevateTextNodes) {

        Node firstChild = XmlUtils.asList(xmlDocument.getChildNodes()).stream()
                .filter(node -> node instanceof Element)
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
        return buildFieldsOf(firstChild, preserveNestedTextInNodes, forceSingleValueNodes, forceElevateTextNodes);
    }

    private Result buildFieldsOf(Node xmlNode,
                                 Set<Node> preserveNestedTextInNodes,
                                 Set<Node> forceSingleValueNodes,
                                 Set<Node> forceElevateTextNodes) {

        if (isElementNode(xmlNode)) {
            Object jsonNode = json.createMap();
            String nodeName = xmlNode.getLocalName();
            NamedNodeMap attributes = xmlNode.getAttributes();
            // apply prefixes to attributes
            IntStream.range(0, attributes.getLength())
                    .mapToObj(attributes::item)
                    .forEach(it -> {
                        String prefix = config.getAttributePrefixesToKeep().contains(it.getPrefix()) ? it.getPrefix() + ":" : "";
                        json.setProperty(jsonNode, config.getAttributeNodePrefix() + prefix + it.getLocalName(), it.getNodeValue());
                    });

            if (preserveNestedTextInNodes.contains(xmlNode)) {
                // this specific text node is configured to always contain nested elements, so render them into a string
                json.setProperty(jsonNode, config.getTextNodeName(), childrenAsXmlString(xmlNode));
                return Result.of(jsonNode, nodeName);
            } else {
                // Within an element, we have:
                //    - text only
                //    - xml tags only
                //    - mixed text with xml tags in it
                boolean nodeHasAnyText = getChildStream(xmlNode)
                        .anyMatch(node -> node instanceof Text);
                boolean nodeHasNonBlankText = getChildStream(xmlNode)
                        .filter(node -> node instanceof Text)
                        .map(node -> (Text) node)
                        .anyMatch(node -> !node.getData().trim().isEmpty());
                boolean nodeHasElements = getChildStream(xmlNode)
                        .filter(node -> node instanceof Element)
                        .anyMatch(XmlUtils::isElementNode);

                if (nodeHasNonBlankText && nodeHasElements) {
                    // This element has: mixed text with xml tags in it
                    // if autodetect is on, great ... if not we must reject the document
                    if (config.isPreserveNestedTextElements_AutoDetect()) {
                        json.setProperty(jsonNode, config.getTextNodeName(), childrenAsXmlString(xmlNode));
                        return Result.of(jsonNode, nodeName);
                    } else {
                        throw new IllegalStateException("Unexpected mixed text/node field at " + getSimpleNodePath(xmlNode));
                    }
                } else if (nodeHasNonBlankText) {
                    // text only
                    String combinedText = getChildStream(xmlNode)
                            .filter(node -> node instanceof Text)
                            .map(node -> (Text) node)
                            .map(Text::getData)
                            .collect(Collectors.joining());
                    json.setProperty(jsonNode, config.getTextNodeName(), combinedText);
                    return Result.of(jsonNode, nodeName);
                } else if (nodeHasElements) {
                    // This element has:  xml tags only  (and maybe some blank text that is ignored)

                    // for elements only, first group the elements by name and make each
                    // group an array even if only with 1 item in it
                    Stream<Node> elementsOnly = getChildStream(xmlNode).filter(node -> node instanceof Element);
                    Stream<Triple<String, Object, Node>> subElementsAsFields = elementsOnly.map(subXmlNode -> {
                        Result subField = buildFieldsOf(subXmlNode, preserveNestedTextInNodes, forceSingleValueNodes, forceElevateTextNodes);
                        if (subField != null) {
                            String subNodeName = subField.getName();
                            Object subJsonNode = subField.getJsonNode();

                            if (forceElevateTextNodes.contains(subXmlNode)) {
                                // elevate the text node into the parent object, if possible.  If conflicting structure, fail the process
                                if (json.isMap(jsonNode)) {
                                    Collection<String> propertyKeys = json.getPropertyKeys(subJsonNode);
                                    if (json.isMap(subJsonNode) &&
                                            config.getTextNodeName().equals(getSingleElementOrNull(propertyKeys))) {
                                        return Triple.of(subNodeName, json.getMapValue(subJsonNode, config.getTextNodeName()), subXmlNode);
                                    } else {
                                        throw new IllegalStateException("Expected only a #text node in " + getSimpleNodePath(subXmlNode));
                                    }
                                } else {
                                    throw new IllegalStateException("Expected parent to be map object to elevate text node into from " + getSimpleNodePath(subXmlNode));
                                }
                            } else {
                                return Triple.of(subNodeName, subJsonNode, subXmlNode);
                            }
                        } else {
                            return null;
                        }
                    }).filter(Objects::nonNull);

                    // subElements become arrays, we group all of the same elements together by the XML node name
                    Map<String, List<Triple<String, Object, Node>>> grouped = subElementsAsFields.collect(groupingBy(Triple::getFirst));
                    Map<String, Object> subElements = grouped.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                                Node relatedXmlNode = e.getValue().get(0).getThird(); // we just need one xml node to check if forced to single value
                                boolean forceToSingleValue = forceSingleValueNodes.contains(relatedXmlNode) ||
                                        (config.isForceElevateTextNodesAreSingleValued() && forceElevateTextNodes.contains(relatedXmlNode));
                                List<Object> innerList = e.getValue().stream().map(Triple::getSecond).collect(toList());
                                Object firstElement = innerList.get(0);
                                boolean isOnlyValueText = innerList.size() == 1 &&
                                        json.isMap(firstElement) &&
                                        config.getTextNodeName().equals(getSingleElementOrNull(json.getPropertyKeys(firstElement)));

                                Object subField;

                                if (forceToSingleValue || isOnlyValueText) {
                                    subField = innerList.get(0);
                                } else {
                                    Object array = json.createArray();
                                    IntStream.range(0, innerList.size())
                                            .forEach(idx -> json.setArrayIndex(array, idx, innerList.get(idx)));
                                    subField = array;
                                }
                                return subField;
                            }));

                    subElements.forEach((key, value) -> json.setProperty(jsonNode, key, value));
                    return Result.of(jsonNode, nodeName);
                } else if (nodeHasAnyText) {
                    // This element has: text only (probably all blank, but we have to render it)
                    json.setProperty(jsonNode, config.getTextNodeName(), xmlNode.getNodeValue());
                    return Result.of(jsonNode, nodeName);
                } else {
                    return null;
                }
            }
        } else if (isTextNode(xmlNode)) {
            throw new IllegalStateException("Text node should have been handled inline with parent element");
        } else if (xmlNode.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE || xmlNode.getNodeType() == Node.COMMENT_NODE) {
            return null;
        } else {
            throw new IllegalStateException("Unknown node type as field, " + xmlNode);
        }
    }

    private static <T> T getSingleElementOrNull(Collection<T> col) {
        if (col.size() != 1) {
            return null;
        } else {
            return col.stream().findFirst().orElse(null);
        }
    }

    @Value(staticConstructor = "of")
    public static class Result {
        Object jsonNode;
        String name;
    }

    @Value(staticConstructor = "of")
    private static class Triple<A, B, C> {
        A first;
        B second;
        C third;
    }
}
