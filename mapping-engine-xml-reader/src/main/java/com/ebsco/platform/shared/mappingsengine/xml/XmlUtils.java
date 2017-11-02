package com.ebsco.platform.shared.mappingsengine.xml;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@UtilityClass
public class XmlUtils {

    public String getSimpleNodePath(Node node) {
        StringJoiner joiner = new StringJoiner("/");
        if (node == null) {
            return "";
        }
        Node parent = node.getParentNode();
        Deque<Node> hierarchy = new ArrayDeque<>();
        while (parent != null) {
            hierarchy.push(parent);
            parent = parent.getParentNode();
        }
        while (!hierarchy.isEmpty()) {
            Node pathPart = hierarchy.pop();
            joiner.add(pathPart.getLocalName());
        }
        return joiner.toString();
    }

    public boolean isTextNode(Node node) {
        return node.getNodeType() == Node.TEXT_NODE;
    }

    public boolean isElementNode(Node node) {
        return node.getNodeType() == Node.ELEMENT_NODE;
    }

    public List<Node> evaluateToNodes(XPathExpression xPathExpression, Document document) {
        try {
            NodeList nodeList = (NodeList) xPathExpression.evaluate(document, XPathConstants.NODESET);
            return asList(nodeList);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Node> asList(NodeList nodeList) {
        return IntStream.range(0, nodeList.getLength())
                .mapToObj(nodeList::item)
                .collect(Collectors.toList());
    }

    public String childrenAsXmlString(Node node) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            StringWriter stringWriter = new StringWriter();
            List<Node> children = asList(node.getChildNodes());
            children.forEach(child -> {
                try {
                    transformer.transform(new DOMSource(child), new StreamResult(stringWriter));
                } catch (TransformerException e) {
                    throw new RuntimeException(e);
                }
            });
            return stringWriter.toString().trim();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void cleanEmptyTextNodes(Node node) {
        NodeList children = node.getChildNodes();
        if (children.getLength() != 1 || children.item(0).getNodeType() != Node.TEXT_NODE) {
            asList(children).forEach(XmlUtils::cleanEmptyTextNodes);
        }
    }

    public Stream<Node> getChildStream(Node xmlNode) {
        return asList(xmlNode.getChildNodes()).stream();
    }

    public Set<Node> xpathToNodes(List<String> nodes, XPath xPath, Document xmlDocument) {
        return nodes.stream()
                .map(it -> compileXpathExpression(it, xPath))
                .flatMap(it -> XmlUtils.evaluateToNodes(it, xmlDocument).stream())
                .collect(toSet());
    }

    @SneakyThrows
    private static XPathExpression compileXpathExpression(String expression, XPath xpath) {
        return xpath.compile(expression);
    }
}
