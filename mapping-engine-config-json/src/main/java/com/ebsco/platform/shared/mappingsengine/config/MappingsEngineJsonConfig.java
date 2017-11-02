package com.ebsco.platform.shared.mappingsengine.config;

import com.ebsco.platform.shared.mappingsengine.config.EngineMetadata;
import com.ebsco.platform.shared.mappingsengine.config.SubsystemConfiguration;
import com.ebsco.platform.shared.mappingsengine.config.TransformsConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.Value;

import java.io.InputStream;
import java.io.Reader;
import java.util.List;

@Value
public class MappingsEngineJsonConfig {

    private static final ObjectMapper jacksonObjectMapper = new ObjectMapper();

    EngineMetadata metadata;
    List<TransformsConfig> transforms;
    SubsystemConfiguration configuration;

    @SneakyThrows
    public static MappingsEngineJsonConfig fromJson(String json) {
        return jacksonObjectMapper.readValue(json, MappingsEngineJsonConfig.class);
    }

    @SneakyThrows
    public static MappingsEngineJsonConfig fromJson(InputStream json) {
        return jacksonObjectMapper.readValue(json, MappingsEngineJsonConfig.class);
    }

    @SneakyThrows
    public static MappingsEngineJsonConfig fromJson(Reader json) {
        return jacksonObjectMapper.readValue(json, MappingsEngineJsonConfig.class);
    }
}
