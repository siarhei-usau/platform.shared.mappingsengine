package com.ebsco.platform.shared.mappingsengine.xml

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.StringWriter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression


internal val Node.simplePath: String
    get() {
        return generateSequence(this) {
            it.parentNode
        }.toList().asReversed().drop(1).map { it.localName }.joinToString("/")

    }

internal val Node.isTextNode get() = nodeType == Node.TEXT_NODE
internal val Node.isElementNode get() = nodeType == Node.ELEMENT_NODE

internal fun XPathExpression.evaluateToNodes(document: Document): List<Node> {
    val results = this.evaluate(document, XPathConstants.NODESET) as NodeList
    return results.toList()
}

internal val Node.childNodeSeq: Sequence<Node> get() = childNodes.toSeq()
internal val Node.childNodeList: List<Node> get() = childNodes.toList()

internal fun Node.asXmlString(): String {
    val transformerFactory = TransformerFactory.newInstance()
    val transformer = transformerFactory.newTransformer().apply {
        setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        setOutputProperty(OutputKeys.INDENT, "no")
    }
    val writer = StringWriter()
    this.childNodeSeq.forEach {
        transformer.transform(DOMSource(it), StreamResult(writer))
        // writer.write(" ")
    }
    return writer.toString().trimIndent()
}

internal fun NodeList.toSeq(): Sequence<Node> = (0..this.length - 1).asSequence().map { this.item(it) }
internal fun NodeList.toList(): List<Node> = (0..this.length - 1).map { this.item(it) }

internal fun Node.cleanEmptyTextNodes() {
    val children = this.childNodes
    if (children.length == 1 && children.item(0).nodeType == Node.TEXT_NODE) {
        if (children.item(0).nodeValue.isBlank()) {
            //   this.removeChild(children.item(0))
        }
    } else {
        children.toSeq().forEach { it.cleanEmptyTextNodes() }
    }
}