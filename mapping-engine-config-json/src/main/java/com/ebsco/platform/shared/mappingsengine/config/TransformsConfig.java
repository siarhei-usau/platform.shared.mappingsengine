package com.ebsco.platform.shared.mappingsengine.config;

import lombok.Value;

import java.util.Map;

@Value
public class TransformsConfig {
    String type;
    String id;
    String notes;
    Map<String, ?> config;
}
