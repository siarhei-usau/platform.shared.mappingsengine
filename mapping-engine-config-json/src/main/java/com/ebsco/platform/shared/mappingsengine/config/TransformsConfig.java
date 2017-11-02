package com.ebsco.platform.shared.mappingsengine.config;

import lombok.NonNull;
import lombok.Value;

import java.util.Map;

@Value
public class TransformsConfig {
    @NonNull
    String type;

    @NonNull
    String id;

    @NonNull
    String notes;

    @NonNull
    Map<String, ?> config;
}
