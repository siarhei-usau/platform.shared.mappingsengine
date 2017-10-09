package com.ebsco.entarch.mappings.streamsets

import com.ebsco.entarch.mappings.xml2json.UniversalNamespaceCache
import com.streamsets.pipeline.api.Field
import org.w3c.dom.*
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory

data class XmlToRecordParserConfig(val preserveNestedTextElements_ByXPath: List<String> = emptyList(),
                                   val preserveNestedTextElements_AutoDetect: Boolean = false,
                                   val preserveNestedTextElements_UnhandledResultInError: Boolean = true) {
    companion object {
        val DEFAULTS = XmlToRecordParserConfig()
    }
}

class XmlToRecordParser(val config: XmlToRecordParserConfig = XmlToRecordParserConfig.DEFAULTS) {

    fun parse(input: InputStream): Pair<String, Field> {
        val builderFactory = DocumentBuilderFactory.newInstance().apply {
            isCoalescing = true
            isIgnoringComments = true
            isNamespaceAware = true
            isValidating = false
        }
        val builder = builderFactory.newDocumentBuilder().apply {
            setEntityResolver(object : EntityResolver {
                override fun resolveEntity(publicId: String?, systemId: String?): InputSource {
                    return InputSource(StringReader(""))
                }
            })
        }
        val xmlDocument = builder.parse(input).apply {
            cleanEmptyTextNodes()
        }

        val xPath = XPathFactory.newInstance().newXPath().apply {
            namespaceContext = UniversalNamespaceCache(xmlDocument, true)
        }

        val preserveNestedTextInNodes = config.preserveNestedTextElements_ByXPath.map { expression ->
            xPath.compile(expression).evaluateToNodes(xmlDocument)
        }.flatten().toSet() // likely identity set, but that is ok


        return buildRootField(xmlDocument, preserveNestedTextInNodes)
    }

    private fun buildRootField(document: Document, preserveNestedTextInNodes: Set<Node>): Pair<String, Field> {
        val firstChild = document.childNodeSeq.filterIsInstance<Element>().first()
        return buildFieldsOf(firstChild, preserveNestedTextInNodes)!!
    }

    private val ATTR_FIELD_PREFIX = "@"
    private val TEXT_FIELD_NAME = "#text"
    private val PROCESS_FIELD_PREFIX = "?"

    private val KEEP_ATTR_PREFIXES = setOf("xmlns", "xml")

    private fun buildFieldsOf(node: Node, preserveNestedTextInNodes: Set<Node>): Pair<String, Field>? {
        val nodeName = node.localName
        val nodeAttrsRaw = node.attributes
        val nodeAttrMap = nodeAttrsRaw?.let { (0..nodeAttrsRaw.length-1).map { nodeAttrsRaw.item(it) }.map {
            val prefix = if (it.prefix in KEEP_ATTR_PREFIXES) it.prefix + ":" else ""
            "$ATTR_FIELD_PREFIX${prefix}${it.localName}" to Field.create(it.nodeValue)
        }.toMap() } ?: emptyMap()

        if (node.isElementNode) {
            if (node in preserveNestedTextInNodes) {
                val textField = TEXT_FIELD_NAME to Field.create(node.asXmlString())
                val objectWithAttributes = nodeName to Field.create(nodeAttrMap + textField)
                return objectWithAttributes
            } else {
                val nodeHasAnyText = node.childNodeSeq.filterIsInstance<Text>().any { true }
                val nodeHasNonBlankText = node.childNodeSeq.filterIsInstance<Text>().any {
                    it.data.isNotBlank()
                }
                val nodeHasElements = node.childNodeSeq.filterIsInstance<Element>().any {
                    it.isElementNode
                }

                if (nodeHasNonBlankText && nodeHasElements) {
                    if (config.preserveNestedTextElements_AutoDetect) {
                        val textField = TEXT_FIELD_NAME to Field.create(node.asXmlString())
                        val objectWithAttributes = nodeName to Field.create(nodeAttrMap + textField)
                        return objectWithAttributes
                    } else {
                        throw IllegalStateException("Unexpected mixed text/node field at ${node.simplePath}")
                    }
                } else {
                    if (nodeHasNonBlankText) {
                        val combinedText = node.childNodeSeq.filterIsInstance<Text>().map {
                            it.data
                        }.joinToString("")
                        val textField = TEXT_FIELD_NAME to Field.create(combinedText)
                        val objectWithAttributes = nodeName to Field.create(nodeAttrMap + textField)
                        return objectWithAttributes
                    } else if (nodeHasElements) {
                        // ok, for elements only, first group the elements by name and make each
                        // group an array even if only with 1 item in it
                        val elementsOnly = node.childNodeSeq.filterIsInstance<Element>()
                        val subElementsAsFields = elementsOnly.map { buildFieldsOf(it, preserveNestedTextInNodes) }
                                .filterNotNull()
                        val groupedSubElementsToArrays = subElementsAsFields.groupBy { it.first }
                                .mapValues {
                                    val innerList = it.value.map { it.second }
                                    val subField = if (innerList.size == 1 && innerList.first().type == Field.Type.MAP
                                            && innerList.first().valueAsMap.size == 1
                                            && innerList.first().valueAsMap.keys.first() == "#text") {
                                        innerList.first()
                                    } else {
                                        Field.create(innerList)
                                    }

                                    subField
                                }

                        val objectWithAttributes = nodeName to Field.create(nodeAttrMap + groupedSubElementsToArrays)

                        return objectWithAttributes
                    } else if (nodeHasAnyText) {
                        val textField = TEXT_FIELD_NAME to Field.create(node.nodeValue)
                        val objectWithAttributes = nodeName to Field.create(nodeAttrMap + textField)
                        return objectWithAttributes
                    } else {
                        return null
                    }
                }
            }
        } else if (node.isTextNode) {
            throw IllegalStateException("Text node should have been handled inline with parent element")
        } else if (node.nodeType == Node.PROCESSING_INSTRUCTION_NODE || node.nodeType == Node.COMMENT_NODE) {
            return null
        } else {
            throw IllegalStateException("Unknown node type as field, $node")
        }
    }

    private val Node.simplePath: String get() {
        return generateSequence(this) {
            it.parentNode
        }.toList().asReversed().drop(1).map { it.localName }.joinToString("/")

    }

    private val Node.isTextNode get() = nodeType == Node.TEXT_NODE
    private val Node.isElementNode get() = nodeType == Node.ELEMENT_NODE

    private fun XPathExpression.evaluateToNodes(document: Document): List<Node> {
        val results = this.evaluate(document, XPathConstants.NODESET) as NodeList
        return results.toList()
    }

    private val Node.childNodeSeq: Sequence<Node> get() = childNodes.toSeq()
    private val Node.childNodeList: List<Node> get() = childNodes.toList()

    private fun Node.asXmlString(): String {
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

    private fun NodeList.toSeq(): Sequence<Node> = (0..this.length-1).asSequence().map { this.item(it) }
    private fun NodeList.toList(): List<Node> = (0..this.length-1).map { this.item(it) }

    private fun Node.cleanEmptyTextNodes() {
        val children = this.childNodes
        if (children.length == 1 && children.item(0).nodeType == Node.TEXT_NODE) {
            if (children.item(0).nodeValue.isBlank()) {
             //   this.removeChild(children.item(0))
            }
        } else {
            children.toSeq().forEach { it.cleanEmptyTextNodes() }
        }
    }
}