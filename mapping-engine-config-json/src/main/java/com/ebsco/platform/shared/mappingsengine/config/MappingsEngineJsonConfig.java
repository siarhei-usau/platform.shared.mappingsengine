package com.ebsco.platform.shared.mappingsengine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class MappingsEngineJsonConfig {
    @NonNull
    private EngineMetadata metadata;

    @NonNull
    private List<TransformsConfig> transforms = new ArrayList<>();

    private SubsystemConfiguration configuration;

    static public MappingsEngineJsonConfig fromJson(String json) throws IOException {
        return new ObjectMapper().readValue(json, MappingsEngineJsonConfig.class);
    }

    static public MappingsEngineJsonConfig fromJson(InputStream json) throws IOException {
        return new ObjectMapper().readValue(json, MappingsEngineJsonConfig.class);
    }

    static public MappingsEngineJsonConfig fromJson(Reader json) throws IOException {
        return new ObjectMapper().readValue(json, MappingsEngineJsonConfig.class);
    }
}