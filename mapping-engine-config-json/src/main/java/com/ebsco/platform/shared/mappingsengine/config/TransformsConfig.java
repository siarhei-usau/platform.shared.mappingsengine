package com.ebsco.platform.shared.mappingsengine.config;

import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Value
public class TransformsConfig {
    @NonNull
    String type;

    @Nullable
    String id;

    @Nullable
    String notes;

    @Nullable
    String testPath;

    @NonNull
    Map<String, ?> config;
}
