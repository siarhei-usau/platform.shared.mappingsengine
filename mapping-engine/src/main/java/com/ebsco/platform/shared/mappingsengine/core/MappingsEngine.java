package com.ebsco.platform.shared.mappingsengine.core;

import com.ebsco.platform.shared.mappingsengine.config.TransformsConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.module.kotlin.ExtensionsKt.jacksonObjectMapper;

/**
 * MappingsEngine applies transformations mutating an existing JSON object
 *
 * All tranformations are plugged in, and are specified in-order within the configuration object
 *
 */
public class MappingsEngine {
    @NonNull
    @Getter
    private final List<TransformsConfig> transforms;

    @NonNull
    @Getter
    private final Map<String, Class<? extends JsonTransformer>> transformerClasses;

    @NonNull
    @Getter
    private JsonProvider jsonProvider;

    // TODO: when no Kotlin is around, this can be just normal `new ObjectMapper()` but doesn't hurt anything either
    private final ObjectMapper mapper = jacksonObjectMapper();

    private final List<JsonTransformer> transformSteps;

    private final Configuration jsonPaths;

    private final Configuration jsonValueList;

    private final Configuration jsonValue;

    public MappingsEngine(final List<TransformsConfig> transforms,
                          final Map<String, Class<? extends JsonTransformer>> transformerClasses,
                          final JsonProvider jsonProvider) {
        this.transforms = new ArrayList<>(transforms);
        this.transformerClasses = transformerClasses;
        this.jsonProvider = jsonProvider;

        this.transformSteps = transforms.stream().map((cfg) -> {
            val transformClass = transformerClasses.get(cfg.getType());
            if (transformClass == null) {
                throw new IllegalStateException("Transformer type ${cfg.type} is not registered!");
            }
            return mapper.convertValue(cfg.getConfig(), transformClass);
        }).collect(Collectors.toList());

        this.jsonPaths = Configuration.builder()
                .options(Option.AS_PATH_LIST, Option.SUPPRESS_EXCEPTIONS)
                .jsonProvider(jsonProvider)
                .build();
        this.jsonValueList = Configuration.builder()
                .options(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS)
                .jsonProvider(jsonProvider)
                .build();

        this.jsonValue = Configuration.builder()
                .options(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS)
                .jsonProvider(jsonProvider)
                .build();
    }


    public void processDocument(@NotNull final Object jsonDocument) {
        final JsonTransformerContext context = new JsonTransformerContext(jsonDocument, jsonPaths, jsonValue, jsonValueList);
        for (JsonTransformer tx : transformSteps) {
            tx.apply(context);
        }
    }
}
