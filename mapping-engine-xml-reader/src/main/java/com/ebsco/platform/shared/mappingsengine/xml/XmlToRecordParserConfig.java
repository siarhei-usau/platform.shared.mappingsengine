package com.ebsco.platform.shared.mappingsengine.xml;

import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;

@Builder
@Getter
public class XmlToRecordParserConfig {

    public static final XmlToRecordParserConfig DEFAULTS = XmlToRecordParserConfig.builder().build();

    @Builder.Default
    List<String> preserveNestedTextElements_ByXPath = emptyList();
    @Builder.Default
    boolean preserveNestedTextElements_AutoDetect = false;
    @Builder.Default
    boolean preserveNestedTextElements_UnhandledResultInError = true;
    @Builder.Default
    List<String> forceSingleValueNodes_ByXPath = emptyList();
    @Builder.Default
    List<String> forceElevateTextNode_ByXPath = emptyList();
    @Builder.Default
    boolean forceElevateTextNodesAreSingleValued = false;
    @Builder.Default
    JsonProvider jsonProvider = new JacksonJsonProvider();
    @Builder.Default
    String textNodeName = "#text";
    @Builder.Default
    String attributeNodePrefix = "@";
    @Builder.Default
    Set<String> attributePrefixesToKeep = Stream.of("xmlns", "xml").collect(toSet());
}
