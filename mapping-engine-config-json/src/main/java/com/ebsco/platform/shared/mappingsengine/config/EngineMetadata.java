package com.ebsco.platform.shared.mappingsengine.config;

import lombok.NonNull;
import lombok.Value;

@Value
public class EngineMetadata {
    @NonNull
    String id;

    @NonNull
    String version;

    @NonNull
    String primaryKey;
}
