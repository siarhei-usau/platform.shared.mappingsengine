package com.ebsco.platform.shared.mappingsengine.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class EngineMetadata {
    @NonNull
    private String id;

    @NonNull
    private String version;

    @NonNull
    private String primaryKey;
}
