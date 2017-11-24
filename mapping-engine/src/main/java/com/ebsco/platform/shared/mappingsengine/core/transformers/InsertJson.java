package com.ebsco.platform.shared.mappingsengine.core.transformers;

import com.ebsco.platform.shared.mappingsengine.core.JsonTransformer;
import com.ebsco.platform.shared.mappingsengine.core.JsonTransformerContext;
import com.ebsco.platform.shared.mappingsengine.core.PathUtils;
import com.ebsco.platform.shared.mappingsengine.core.ResolvedPaths;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jayway.jsonpath.JsonPath;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InsertJson implements JsonTransformer {
    @NonNull
    private String targetPath;

    @NonNull
    private Object jsonFragment;

    @JsonCreator
    public InsertJson(@NotNull @JsonProperty("targetPath") String targetPath, @NotNull @JsonProperty("jsonFragment") Object jsonFragment) {
        if (!targetPath.startsWith("$")) throw new IllegalArgumentException("targetPath must be absolute path");
        this.targetPath = targetPath;
        this.jsonFragment = jsonFragment;
    }

    @Override
    public void apply(@NotNull JsonTransformerContext context) {
        List<ResolvedPaths> fromToMapping = context.queryAndResolveTargetPaths("$", targetPath, true);
        fromToMapping.forEach(mapping -> {
            context.applyUpdate(mapping, jsonFragment);
        });
    }
}
