package com.ebsco.platform.shared.mappingsengine.config;

import lombok.Value;

@Value
public class EngineMetadata {
    String id;
    String version;
    String primaryKey;
}
