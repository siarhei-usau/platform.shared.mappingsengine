package com.ebsco.platform.shared.mappingsengine.core;

import com.ebsco.platform.shared.mappingsengine.config.TransformsConfig;
import com.ebsco.platform.shared.mappingsengine.core.transformers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

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
    private final List<JsonTransformer> transformSteps = createTransformSteps();

    private List<JsonTransformer> createTransformSteps() {
        return transforms.stream().map(cfg -> {
            Class<? extends JsonTransformer> transformClass = transformerClasses.get(cfg.getType());
            if (transformClass == null) {
                throw new IllegalStateException("Transformer type " + cfg.getType() + " is not registered!");
            }
            return mapper.convertValue(cfg.getConfig(), transformClass);
        }).collect(toList());
    }

    public void processDocument(Object jsonDocument) {
        JsonTransformerContext context = new JsonTransformerContext(jsonDocument, jsonPaths, jsonValue, jsonValueList);
        getTransformSteps().forEach(step -> step.apply(context));
    }

    private static Map<String, Class<? extends JsonTransformer>> registerTransformers() {
        Map<String, Class<? extends JsonTransformer>> result = new HashMap<>();
        result.put("rename", RenameJson.class);
        result.put("copy", CopyJson.class);
        result.put("delete", DeleteJson.class);
        result.put("concatBroken", ConcatJson.class);
        result.put("concat", OriginalConcatJson.class);
        result.put("lookup", LookupJson.class);
        return result;
    }

}
