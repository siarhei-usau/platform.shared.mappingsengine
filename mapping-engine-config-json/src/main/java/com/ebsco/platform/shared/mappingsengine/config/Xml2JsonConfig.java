package com.ebsco.platform.shared.mappingsengine.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
public class Xml2JsonConfig {
    @NonNull
    private List<String> embedLiteralXmlAtPaths = new ArrayList<>();

    private boolean autoDetectMixedContent = false;

    private boolean unhandledMixedContentIsError = true;

    @NonNull
    private List<String> forceSingleValueElementAtPaths = new ArrayList<>();

    @NonNull
    private List<String> forceElevateTextNodesAtPaths = new ArrayList<>();

    private boolean forceElevateTextNodesAsSingleValue = false;

    @NonNull
    private String textNodeName = "value";

    @NonNull
    private String attributeNodePrefix = "";

    @NonNull
    private List<String> preserveAttributePrefixes = Arrays.asList("xmlns", "xml");
}

