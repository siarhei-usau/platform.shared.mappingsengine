package com.ebsco.platform.shared.mappingsengine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class TransformsConfig {
    @NonNull
    private String type;

    private String id;

    private String notes;

    @NonNull
    private Map<String, Object> config = new HashMap<>();

    public <T> T bind(final Class<T> toClass) {
        return new ObjectMapper().convertValue(config, toClass);
    }
}
