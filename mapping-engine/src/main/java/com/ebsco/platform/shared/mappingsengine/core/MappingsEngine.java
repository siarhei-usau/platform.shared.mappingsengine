package com.ebsco.platform.shared.mappingsengine.core;

import com.ebsco.platform.shared.mappingsengine.config.TransformsConfig;
import com.ebsco.platform.shared.mappingsengine.core.transformers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Builder
public class MappingsEngine {

    private static final Map<String, Class<? extends JsonTransformer>> REGISTERED_TRANSFORMERS = registerTransformers();
    @NonNull
    @Getter
    private List<TransformsConfig> transforms;
    @Getter
    @Builder.Default
    private JsonProvider jsonProvider = new JacksonJsonProvider();
    @Getter
    @Builder.Default
    private Map<String, Class<? extends JsonTransformer>> transformerClasses = REGISTERED_TRANSFORMERS;

    private final Configuration jsonValueList = Configuration.builder()
            .options(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS)
            .jsonProvider(jsonProvider)
            .build();

    private final Configuration jsonPaths = Configuration.builder()
            .options(Option.AS_PATH_LIST, Option.SUPPRESS_EXCEPTIONS)
            .jsonProvider(jsonProvider)
            .build();

    private final Configuration jsonValue = Configuration.builder()
            .options(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS)
            .jsonProvider(jsonProvider)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    @Getter(lazy = true)
    private final List<ConfiguredTransform> transformSteps = createTransformSteps();

    public static class ConfiguredTransform {
        public TransformsConfig config;
        public JsonTransformer instance;
        public JsonPath compiledTestPath = null;

        public ConfiguredTransform(final TransformsConfig config, final JsonTransformer transformer) {
            this.config = config;
            this.instance = transformer;

            if (config.getTestPath() != null) {
                this.compiledTestPath = JsonPath.compile(config.getTestPath());
            }
        }
    }

    private List<ConfiguredTransform> createTransformSteps() {
        return transforms.stream().map(cfg -> {
                Class<? extends JsonTransformer> transformClass = transformerClasses.get(cfg.getType());
                if (transformClass == null) {
                    throw new IllegalStateException("Transformer type " + cfg.getType() + " is not registered!");
                }
                return new ConfiguredTransform(cfg, mapper.convertValue(cfg.getConfig(), transformClass));
            }).collect(toList());
    }

    public void processDocument(Object jsonDocument) {
        JsonTransformerContext context = new JsonTransformerContext(jsonDocument, jsonPaths, jsonValue, jsonValueList);
        getTransformSteps().stream().filter(step -> {
            if (step.compiledTestPath != null) {
                List<String> checkPaths = context.queryForPaths(step.compiledTestPath);
                return !checkPaths.isEmpty();
            }
            else {
                return true;
            }
        }).forEach(step -> step.instance.apply(context));
    }

    private static Map<String, Class<? extends JsonTransformer>> registerTransformers() {
        return DefaultTransformers.TRANFORMERS;
    }

}
