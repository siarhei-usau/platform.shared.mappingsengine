package com.ebsco.platform.shared.mappingsengine.config;

import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;

@Getter
@ToString
public class Xml2JsonConfig {
    List<String> embedLiteralXmlAtPaths = emptyList();
    boolean autoDetectMixedContent = false;
    boolean unhandledMixedContentIsError = true;
    List<String> forceSingleValueElementAtPaths = emptyList();
    List<String> forceElevateTextNodesAtPaths = emptyList();
    boolean forceElevateTextNodesAsSingleValue = false;
    String textNodeName = "value";
    String attributeNodePrefix = "";
    Set<String> preserveAttributePrefixes = Stream.of("xmlns", "xml").collect(toSet());
}
