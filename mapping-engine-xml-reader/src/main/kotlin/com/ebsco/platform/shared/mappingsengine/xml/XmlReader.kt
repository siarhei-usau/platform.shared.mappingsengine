package com.ebsco.platform.shared.mappingsengine.xml

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import java.io.InputStream
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory


class XmlToRecordParser(val config: XmlToRecordParserConfig = XmlToRecordParserConfig.DEFAULTS) {
    private val JSON = config.jsonProvider

    fun parse(input: InputStream): Pair<String, Any> {
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
            namespaceContext = XmlNamespaceCache(xmlDocument, true)
        }

        fun List<String>.xpathToNodes(): Set<Node> {
            return this.map { xPath.compile(it).evaluateToNodes(xmlDocument) }.flatten().toSet() // likely identity set, but that is ok
        }

        val preserveNestedTextInNodes = config.preserveNestedTextElements_ByXPath.xpathToNodes()
        val forceSingleValueNodes = config.forceSingleValueNodes_ByXPath.xpathToNodes()
        val forceElevateTextNodes = config.forceElevateTextNode_ByXPath.xpathToNodes()

        return buildRootField(xmlDocument, preserveNestedTextInNodes, forceSingleValueNodes, forceElevateTextNodes)
    }

    private fun buildRootField(document: Document,
                               preserveNestedTextInNodes: Set<Node>,
                               forceSingleValueNodes: Set<Node>,
                               forceElevateTextNodes: Set<Node>): Pair<String, Any> {
        val firstChild = document.childNodeSeq.filterIsInstance<Element>().first()
        return buildFieldsOf(firstChild, preserveNestedTextInNodes, forceSingleValueNodes, forceElevateTextNodes)!!
    }

    private fun buildFieldsOf(xmlNode: Node,
                              preserveNestedTextInNodes: Set<Node>,
                              forceSingleValueNodes: Set<Node>,
                              forceElevateTextNodes: Set<Node>): Pair<String, Any>? {
        if (xmlNode.isElementNode) {
            val jsonNode = JSON.createMap()
            val nodeName = xmlNode.localName
            xmlNode.attributes?.also { nodeAttrsRaw ->
                (0..nodeAttrsRaw.length - 1).map { nodeAttrsRaw.item(it) }.forEach {
                    val prefix = if (it.prefix in config.attributePrefixesToKeep) it.prefix + ":" else ""
                    JSON.setProperty(jsonNode, "${config.attributeNodePrefix}${prefix}${it.localName}", it.nodeValue)
                }
            }

            if (xmlNode in preserveNestedTextInNodes) {
                // this specific text node is configured to always contain nested elements, so render them into a string
                JSON.setProperty(jsonNode, config.textNodeName, xmlNode.asXmlString())
                return Pair(nodeName, jsonNode)
            } else {
                // Within an element, we have:
                //    - text only
                //    - xml tags only
                //    - mixed text with xml tags in it

                val nodeHasAnyText = xmlNode.childNodeSeq.filterIsInstance<Text>().any { true }
                val nodeHasNonBlankText = xmlNode.childNodeSeq.filterIsInstance<Text>().any {
                    it.data.isNotBlank()
                }
                val nodeHasElements = xmlNode.childNodeSeq.filterIsInstance<Element>().any {
                    it.isElementNode
                }

                if (nodeHasNonBlankText && nodeHasElements) {
                    // This element has: mixed text with xml tags in it
                    // if autodetect is on, great ... if not we must reject the document
                    if (config.preserveNestedTextElements_AutoDetect) {
                        JSON.setProperty(jsonNode, config.textNodeName, xmlNode.asXmlString())
                        return Pair(nodeName, jsonNode)
                    } else {
                        throw IllegalStateException("Unexpected mixed text/node field at ${xmlNode.simplePath}")
                    }
                } else {
                    if (nodeHasNonBlankText) {
                        // This element has: text only
                        val combinedText = xmlNode.childNodeSeq.filterIsInstance<Text>().map {
                            it.data
                        }.joinToString("")

                        JSON.setProperty(jsonNode, config.textNodeName, combinedText)
                        return Pair(nodeName, jsonNode)
                    } else if (nodeHasElements) {
                        // This element has:  xml tags only  (and maybe some blank text that is ignored)

                        // ok, for elements only, first group the elements by name and make each
                        // group an array even if only with 1 item in it
                        val elementsOnly = xmlNode.childNodeSeq.filterIsInstance<Element>()

                        val subElementsAsFields = elementsOnly.map { subXmlNode ->
                             val subField = buildFieldsOf(subXmlNode, preserveNestedTextInNodes, forceSingleValueNodes, forceElevateTextNodes)
                             if (subField != null) {
                                 val (subNodeName, subJsonNode) = subField
                                 if (subXmlNode in forceElevateTextNodes) {
                                     // elevate the text node into the parent object, if possible.  If conflicting structure, fail the process
                                     if (JSON.isMap(jsonNode)) {
                                         if (JSON.isMap(subJsonNode) 
                                                 && config.textNodeName == JSON.getPropertyKeys(subJsonNode).singleOrNull()) {
                                             Triple(subNodeName, JSON.getMapValue(subJsonNode, config.textNodeName), subXmlNode)
                                         } else {
                                             throw IllegalStateException("Expected only a #text node in ${subXmlNode.simplePath}")
                                         }
                                     } else {
                                         throw IllegalStateException("Expected parent to be map object to elevate text node into from ${subXmlNode.simplePath}")
                                     }
                                 } else {
                                     Triple(subNodeName, subJsonNode, subXmlNode)
                                 }
                             } else {
                                 null
                             }
                        }.filterNotNull()

                        // subElements become arrays, we group all of the same elements together by the XML node name
                        val subElements = subElementsAsFields.groupBy { it.first }
                                .mapValues {
                                    val relatedXmlNode = it.value.first().third // we just need one xml node to check if forced to single value
                                    val forceToSingleValue = relatedXmlNode in forceSingleValueNodes ||
                                            (config.forceElevateTextNodesAreSingleValued && relatedXmlNode in forceElevateTextNodes)

                                    val innerList = it.value.map { it.second }
                                    val isOnlyValueText = innerList.size == 1
                                            && JSON.isMap(innerList.first())
                                            && config.textNodeName == JSON.getPropertyKeys(innerList.first()).singleOrNull()

                                    val subField = if (forceToSingleValue ||isOnlyValueText ) {
                                        innerList.first()
                                    } else {
                                        val innerArray = JSON.createArray().apply {
                                            innerList.forEachIndexed { idx, item ->
                                                JSON.setArrayIndex(this, idx, item)
                                            }
                                        }
                                        innerArray
                                    }

                                    subField
                                }

                        subElements.forEach {
                            JSON.setProperty(jsonNode, it.key, it.value)
                        }

                        return Pair(nodeName, jsonNode)
                    } else if (nodeHasAnyText) {
                        // This element has: text only (probably all blank, but we have to render it)
                        JSON.setProperty(jsonNode, config.textNodeName, xmlNode.nodeValue)
                        return Pair(nodeName, jsonNode)
                    } else {
                        return null
                    }
                }
            }
        } else if (xmlNode.isTextNode) {
            throw IllegalStateException("Text node should have been handled inline with parent element")
        } else if (xmlNode.nodeType == Node.PROCESSING_INSTRUCTION_NODE || xmlNode.nodeType == Node.COMMENT_NODE) {
            return null
        } else {
            throw IllegalStateException("Unknown node type as field, $xmlNode")
        }
    }

}