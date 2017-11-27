package com.ebsco.platform.shared.mappingsengine.core.transformers;

import com.ebsco.platform.shared.mappingsengine.core.JsonTransformer;
import com.ebsco.platform.shared.mappingsengine.core.JsonTransformerContext;
import com.ebsco.platform.shared.mappingsengine.core.ResolvedPaths;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PivotJson implements JsonTransformer {
    @NonNull
    private String fromPath;

    @NonNull
    private String targetPath;

    @NonNull
    private String keyField;

    @NonNull
    private String valueField;

    private JsonPath compiledSourceJsonPath;

    @JsonCreator
    public PivotJson(@NotNull @JsonProperty("fromPath") String fromPath,
                     @NotNull @JsonProperty("targetPath") String targetPath,
                     @NotNull @JsonProperty("keyField") String keyField,
                     @NotNull @JsonProperty("valueField") String valueField) {
        this.fromPath = fromPath;
        if (!targetPath.startsWith("$")) throw new IllegalArgumentException("targetPath must be absolute path");
        this.targetPath = targetPath;
        this.keyField = keyField;
        this.valueField = valueField;
        this.compiledSourceJsonPath = JsonPath.compile(this.fromPath);
    }

    @Override
    public void apply(@NotNull JsonTransformerContext context) {
        List<ResolvedPaths> fromToMapping = context.queryAndResolveTargetPaths("$", targetPath);
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        context.queryForValues(compiledSourceJsonPath).forEach(v -> {
            val abstractTypeFieldValue = context.getJpathCtx().configuration().jsonProvider().getMapValue(v, keyField);
            val abstractFieldValue = context.getJpathCtx().configuration().jsonProvider().getMapValue(v, valueField);
            node.put(abstractTypeFieldValue.toString(), abstractFieldValue.toString());
        });
        fromToMapping.forEach(mapping -> {
            context.applyUpdate(mapping, node);
        });
    }
}
