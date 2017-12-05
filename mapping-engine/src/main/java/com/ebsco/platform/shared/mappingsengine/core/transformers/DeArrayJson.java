package com.ebsco.platform.shared.mappingsengine.core.transformers;

import com.ebsco.platform.shared.mappingsengine.core.JsonTransformer;
import com.ebsco.platform.shared.mappingsengine.core.JsonTransformerContext;
import com.ebsco.platform.shared.mappingsengine.core.ResolvedPaths;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.jsonpath.JsonPath;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeArrayJson implements JsonTransformer {
    @NonNull
    private String fromPath;

    @NonNull
    private String targetPath;

    @NonNull
    private String keyField;

    private JsonPath compiledSourceJsonPath;

    private ObjectMapper mapper = new ObjectMapper();

    @JsonCreator
    public DeArrayJson(@NotNull @JsonProperty("fromPath") String fromPath,
                       @NotNull @JsonProperty("keyField") String keyField) {
        this.fromPath = fromPath;
        this.targetPath = fromPath;
        this.compiledSourceJsonPath = JsonPath.compile(this.fromPath);
        this.keyField = keyField;
    }

    @Override
    public void apply(@NotNull JsonTransformerContext context) {

        List<ResolvedPaths> fromToMapping = context.queryAndResolveTargetPaths(compiledSourceJsonPath, targetPath);
        fromToMapping.forEach(mapping -> {
            Object sourceValue = context.queryForValue(mapping.getSourcePath());

            if (context.getJpathCtx().configuration().jsonProvider().isMap(sourceValue) && context.getJpathCtx().configuration().jsonProvider().getPropertyKeys(sourceValue).contains(keyField)) {
                Map map = mapper.convertValue(sourceValue, Map.class);
                map.forEach((mapKey, mapValue) -> {
                    if (mapValue instanceof ArrayList) {
                        if (((ArrayList) mapValue).size() == 1) {
                            map.put(keyField, ((ArrayList) mapValue).get(0));
                        }
                    }
                });
                context.applyUpdate(mapping, map);
            }
            if (sourceValue instanceof ArrayNode) {
                List list = mapper.convertValue(sourceValue, List.class);
                list.forEach(item -> {
                    if (context.getJpathCtx().configuration().jsonProvider().getPropertyKeys(item).contains(keyField)) {
                        Object value = context.getJpathCtx().configuration().jsonProvider().getMapValue(item, keyField);
                        if (value instanceof ArrayList) {
                            if (((ArrayList) value).size() == 1) {
                                context.getJpathCtx().configuration().jsonProvider().setProperty(item, keyField, ((ArrayList) value).get(0));
                            }
                        }
                    }
                });
                context.applyUpdate(mapping, list);
            }
        });
    }
}
