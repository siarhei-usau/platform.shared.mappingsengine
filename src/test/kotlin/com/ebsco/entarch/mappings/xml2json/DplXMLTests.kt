package com.ebsco.entarch.mappings.xml2json

import org.junit.Test
import kotlin.test.assertEquals

class DplXMLTests {
    @Test
    fun testBasicConversion() {
        val xml = """
            <something>
               <title>Happy Document</title>
               <id type="doi">912399.11</id>
               <section>
                  <part no="1">
                     <p>This is a paragraph with <b>something bolded</b> and we want to <i>emphasize</i> that this doesn't change</p>
                  </part>
                  <part no="2">
                     <p>This is obviously the second paragraph</p>
                  </part>
                  <part no="3">
                     <p>en fin</p>
                  </part>
               </section>
            </something>
            """

        val expectedJSON = """
            {"something":{"section":{"part":[{"p":{"b":{"#text":"something bolded"},"#text":["This is a paragraph with","and we want to","that this doesn't change"],"i":{"#text":"emphasize"}},"@no":"1"},{"p":{"#text":"This is obviously the second paragraph"},"@no":"2"},{"p":{"#text":"en fin"},"@no":"3"}]},"id":{"#text":"912399.11","@type":"doi"},"title":{"#text":"Happy Document"}}}
            """.trim()

        assertEquals(expectedJSON, DplXML.toJSONObject(xml).toString())
    }
}