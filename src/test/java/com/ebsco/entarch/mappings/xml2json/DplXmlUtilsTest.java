package com.ebsco.entarch.mappings.xml2json;

/*
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class DplXmlUtilsTest {

    @Test
    public void testRemoveBOM() throws Exception {
        File file = new ClassPathResource("/data/xml/bom.xml").getFile();
        String inputXml = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        int oldLength = inputXml.length();
        String newXml = DplXmlUtils.removeBOM(inputXml);
        int newLength = newXml.length();
        assertEquals(oldLength, newLength + 1);
    }

    // test to selectively preserve mixed content
    @Test
    public void testMixedEdsatm() throws Exception {
        File file = new ClassPathResource("/data/Camel/xml/mixed/135_1.xml").getFile();
        String inputXml = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        String newXml = DplXmlUtils.escapeXmlTags(inputXml, Arrays.asList("p"));
        int oldCnt = StringUtils.countMatches(newXml, DplConstant.XML_ESCAPE_LEFT);
        System.out.println(oldCnt);
        newXml = DplXmlUtils.escapeXmlTags(inputXml, Arrays.asList("p", "refauth"));
        int newCnt = StringUtils.countMatches(newXml, DplConstant.XML_ESCAPE_LEFT);
        System.out.println(newCnt);
        assertTrue(newCnt > oldCnt);
    }

    @Test
    public void testCDATAMixed() throws Exception {
        File file = new ClassPathResource("/data/Camel/xml/mixed/cdataMixed.xml").getFile();
        String inputXml = FileUtils.readFileToString(file);
        boolean hasMixed = DplXmlUtils.hasMixedContent(inputXml);
        assertFalse(hasMixed);

        String newXml = DplXmlUtils.escapeMixedContent(inputXml);
        assertTrue(newXml.contains("<![CDATA["));
        assertTrue(newXml.contains("]]>"));
    }

    @Test
    public void testMixedContent() throws Exception {
        File file = new ClassPathResource("/data/Camel/xml/mixed/mixed.xml").getFile();
        String inputXml = FileUtils.readFileToString(file);
        boolean hasMixed = DplXmlUtils.hasMixedContent(inputXml);
        assertTrue(hasMixed);

        String newXml = DplXmlUtils.escapeMixedContent(inputXml);
        assertTrue(newXml.contains(DplConstant.XML_ESCAPE_LEFT));
        assertTrue(newXml.contains(DplConstant.XML_ESCAPE_RIGHT));

        newXml = DplXmlUtils.unescapeMixedContent(inputXml);
        assertFalse(newXml.contains(DplConstant.XML_ESCAPE_LEFT));
        assertFalse(newXml.contains(DplConstant.XML_ESCAPE_RIGHT));
        assertTrue(newXml.contains("<italic>"));
        assertTrue(newXml.contains("</italic>"));
        assertTrue(newXml.contains("<sup>"));
        assertTrue(newXml.contains("</sup>"));
    }

    @Test
    public void testEscapeAmp() {
        String xml = "<p>&amp;lt;&ampgt;";
        String expected = "<p>" + DplConstant.XML_ESCAPE_AMP + "amp;lt;" + DplConstant.XML_ESCAPE_AMP + "ampgt;";
        System.out.println(DplXmlUtils.escapeAmp(xml));
        assertEquals(expected, DplXmlUtils.escapeAmp(xml));
        assertEquals(xml, DplXmlUtils.unescapeAmp(expected));
    }

    @Test
    public void testFilterTag() throws Exception {
        File file = new ClassPathResource("/data/Camel/xml/tag/tag.xml").getFile();
        String inputXml = FileUtils.readFileToString(file);

        System.out.println(inputXml);
        String newXml = DplXmlUtils.filterXmlTags(inputXml, Arrays.asList("body", "book-last"));
        System.out.println(newXml);
        assertTrue(newXml.contains(DplConstant.DPL_FILTERED_CONTENT));
        assertTrue(newXml.contains("<body>" + DplConstant.DPL_FILTERED_CONTENT + "</body>"));
        assertTrue(newXml.contains("<book-last>" + DplConstant.DPL_FILTERED_CONTENT + "</book-last>"));
    }

    @Test
    public void testFilterCdataTag() throws Exception {
        File file = new ClassPathResource("/data/Camel/xml/tag/cdataTag.xml").getFile();
        String inputXml = FileUtils.readFileToString(file);

        System.out.println(inputXml);
        String newXml = DplXmlUtils.filterXmlTags(inputXml, Arrays.asList("writers", "copyrightHolder"));
        System.out.println(newXml);
        assertTrue(newXml.contains(DplConstant.DPL_FILTERED_CONTENT));
        assertTrue(newXml.contains("<copyrightHolder>" + DplConstant.DPL_FILTERED_CONTENT + "</copyrightHolder>"));
        assertTrue(newXml.contains("<writers>" + DplConstant.DPL_FILTERED_CONTENT + "</writers>"));
    }

    @Test
    public void testEscapeTag() throws Exception {
        File file = new ClassPathResource("/data/Camel/xml/tag/tag.xml").getFile();
        String inputXml = FileUtils.readFileToString(file);

        System.out.println(inputXml);
        String newXml = DplXmlUtils.escapeXmlTags(inputXml, Arrays.asList("body", "book-last"));
        System.out.println(newXml);
        assertTrue(newXml.contains(DplConstant.XML_ESCAPE_LEFT));
        assertTrue(newXml.contains(DplConstant.XML_ESCAPE_RIGHT));
    }

    @Test
    public void testEscapeCdataTag() throws Exception {
        File file = new ClassPathResource("/data/Camel/xml/tag/cdataTag.xml").getFile();
        String inputXml = FileUtils.readFileToString(file);

        System.out.println(inputXml);
        String newXml = DplXmlUtils.escapeXmlTags(inputXml, Arrays.asList("writers", "copyrightHolder"));
        System.out.println(newXml);
        assertTrue(newXml.contains(DplConstant.XML_ESCAPE_LEFT));
        assertTrue(newXml.contains(DplConstant.XML_ESCAPE_RIGHT));
        assertTrue(newXml.contains(DplConstant.XML_ESCAPE_LEFT + "![CDATA[Bob]]" + DplConstant.XML_ESCAPE_RIGHT));
    }

    @Test
    public void testEscapeUnescapeBrackets() throws Exception {
        String s = "<abc></abc>";
        String rs = DplXmlUtils.escapeBracket(s);
        System.out.println(rs);
        rs = DplXmlUtils.unescapeBracket(rs);
        System.out.println(rs);
        assertEquals(s, rs);
    }

    @Test
    public void testUnescapeXml() {
        String inputXml = "&amp;amp;";
        assertEquals("&amp;", DplXmlUtils.unescapeXml(inputXml, false));
        assertEquals("&", DplXmlUtils.unescapeXml(inputXml, true));
    }

    @Test
    public void testUnescapeEntity() {
        String inputXml = "&#38;rsquo;";
        assertEquals("&rsquo;", DplXmlUtils.unescapeEntity(inputXml, false));
        assertEquals("’", DplXmlUtils.unescapeEntity(inputXml, true));
    }

    @Test
    public void testEscapeEhostEntity2() {
        String s = "a<b><i>c</i>";
        String expected = "a<b>" + DplConstant.XML_ESCAPE_LEFT + "i" + DplConstant.XML_ESCAPE_RIGHT + "c"
                + DplConstant.XML_ESCAPE_LEFT + "/i" + DplConstant.XML_ESCAPE_RIGHT;
        String actual = DplXmlUtils.escapeEhostTag(s);
        System.out.println(actual);
        assertEquals(expected, actual);
    }

    public static void main(String[] args) {
        System.out.println(DplXmlUtils.escapeEhostTag("<b xml='lang'/>"));
        System.out.println(DplXmlUtils.escapeEhostTag("<b/>"));
    }

    @Test
    public void testEscapeEhostEntitiy() {
        for (String entity : DplConstant.EHOST_TAGS) {
            String s = "<" + entity + ">";
            String expected = s;
            assertEquals(expected, DplXmlUtils.escapeEhostTag(s));

            s = "<" + "/" + entity + ">";
            expected = s;
            assertEquals(expected, DplXmlUtils.escapeEhostTag(s));

            s = "<" + entity + ">a</" + entity + ">";
            expected = DplConstant.XML_ESCAPE_LEFT + entity + DplConstant.XML_ESCAPE_RIGHT + "a"
                    + DplConstant.XML_ESCAPE_LEFT + "/" + entity + DplConstant.XML_ESCAPE_RIGHT;
            assertEquals(expected, DplXmlUtils.escapeEhostTag(s));

            s = "<" + entity + "/" + ">";
            expected = DplConstant.XML_ESCAPE_LEFT + entity + "/" + DplConstant.XML_ESCAPE_RIGHT;
            assertEquals(expected, DplXmlUtils.escapeEhostTag(s));

            s = "<" + entity + " /" + ">";
            expected = DplConstant.XML_ESCAPE_LEFT + entity + " /" + DplConstant.XML_ESCAPE_RIGHT;
            assertEquals(expected, DplXmlUtils.escapeEhostTag(s));

            s = "<" + entity + "  /" + ">";
            expected = DplConstant.XML_ESCAPE_LEFT + entity + "  /" + DplConstant.XML_ESCAPE_RIGHT;
            assertEquals(expected, DplXmlUtils.escapeEhostTag(s));

            s = "<" + entity;
            expected = s;
            assertEquals(expected, DplXmlUtils.escapeEhostTag(s));

            s = "<abc>" + entity + "</def>";
            expected = s;
            assertEquals(expected, DplXmlUtils.escapeEhostTag(s));

            s = "<abc> " + entity + " </def>";
            expected = s;
            assertEquals(expected, DplXmlUtils.escapeEhostTag(s));
        }
        String mixedString = "test <i>i</i><b>b</b><small>s</small><sup>sup</sup><sub>sub</sub><tt>t</tt><br/> done";
        String expected = "test DPL_XML_ESCAPE_LEFTiDPL_XML_ESCAPE_RIGHTiDPL_XML_ESCAPE_LEFT/iDPL_XML_ESCAPE_RIGHTDPL_XML_ESCAPE_LEFTbDPL_XML_ESCAPE_RIGHTbDPL_XML_ESCAPE_LEFT/bDPL_XML_ESCAPE_RIGHTDPL_XML_ESCAPE_LEFTsmallDPL_XML_ESCAPE_RIGHTsDPL_XML_ESCAPE_LEFT/smallDPL_XML_ESCAPE_RIGHTDPL_XML_ESCAPE_LEFTsupDPL_XML_ESCAPE_RIGHTsupDPL_XML_ESCAPE_LEFT/supDPL_XML_ESCAPE_RIGHTDPL_XML_ESCAPE_LEFTsubDPL_XML_ESCAPE_RIGHTsubDPL_XML_ESCAPE_LEFT/subDPL_XML_ESCAPE_RIGHTDPL_XML_ESCAPE_LEFTttDPL_XML_ESCAPE_RIGHTtDPL_XML_ESCAPE_LEFT/ttDPL_XML_ESCAPE_RIGHTDPL_XML_ESCAPE_LEFTbr/DPL_XML_ESCAPE_RIGHT done";
        System.out.println(expected);
        String actual = DplXmlUtils.escapeEhostTag(mixedString);
        System.out.println(actual);
        assertEquals(expected, actual);

        String x = "abc<![CDATA[a<i>b</i>c]]>def";
        expected = "abcDPL_XML_ESCAPE_LEFT![CDATA[aDPL_XML_ESCAPE_LEFTiDPL_XML_ESCAPE_RIGHTbDPL_XML_ESCAPE_LEFT/iDPL_XML_ESCAPE_RIGHTc]]DPL_XML_ESCAPE_RIGHTdef";
        actual = DplXmlUtils.escapeEhostTag(x);
        System.out.println(x);
        System.out.println(actual);
        assertEquals(expected, actual);

        x = "a<b>b</b>c<n><![CDATA[a<i>b</i>c]]>a<b>b</b>c<n><br><br/><br /><br  /><a></a></der>";
        expected = "aDPL_XML_ESCAPE_LEFTbDPL_XML_ESCAPE_RIGHTbDPL_XML_ESCAPE_LEFT/bDPL_XML_ESCAPE_RIGHTc<n>DPL_XML_ESCAPE_LEFT![CDATA[aDPL_XML_ESCAPE_LEFTiDPL_XML_ESCAPE_RIGHTbDPL_XML_ESCAPE_LEFT/iDPL_XML_ESCAPE_RIGHTc]]DPL_XML_ESCAPE_RIGHTaDPL_XML_ESCAPE_LEFTbDPL_XML_ESCAPE_RIGHTbDPL_XML_ESCAPE_LEFT/bDPL_XML_ESCAPE_RIGHTc<n><br>DPL_XML_ESCAPE_LEFTbr/DPL_XML_ESCAPE_RIGHTDPL_XML_ESCAPE_LEFTbr /DPL_XML_ESCAPE_RIGHTDPL_XML_ESCAPE_LEFTbr  /DPL_XML_ESCAPE_RIGHT<a></a></der>";
        actual = DplXmlUtils.escapeEhostTag(x);
        System.out.println(x);
        System.out.println(actual);
        assertEquals(expected, actual);
    }

    @Test
    public void testIsValidXml() throws IOException {
        String marcXml = FileUtils.readFileToString(new ClassPathResource("/data/xml/epmarc.xml").getFile(),
                StandardCharsets.UTF_8);
        System.out.println(marcXml);
        assertTrue(DplXmlUtils.isValidXml(marcXml));
        String badMarcXml = FileUtils.readFileToString(new ClassPathResource("/data/xml/epmarc_bad.xml").getFile(),
                StandardCharsets.UTF_8);
        assertFalse(DplXmlUtils.isValidXml(badMarcXml));
        String abodyXml = FileUtils.readFileToString(new ClassPathResource("/data/xml/abody.xml").getFile(),
                StandardCharsets.UTF_8);
        assertTrue(DplXmlUtils.isValidXml(abodyXml));
        String badAbodyXml = FileUtils.readFileToString(new ClassPathResource("/data/xml/abody_bad.xml").getFile(),
                StandardCharsets.UTF_8);
        assertFalse(DplXmlUtils.isValidXml(badAbodyXml));
    }

}
*/