package com.ebsco.platform.shared.mappingsengine.core.transformers;

import com.ebsco.platform.shared.mappingsengine.core.JsonTransformer;
import com.ebsco.platform.shared.mappingsengine.core.JsonTransformerContext;
import com.ebsco.platform.shared.mappingsengine.core.ResolvedPaths;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jayway.jsonpath.JsonPath;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CopyJson implements JsonTransformer {

    @NonNull
    private String fromPath;

    @NonNull
    private String targetPath;

    private JsonPath compiledSourceJsonPath;

    @JsonCreator
    public CopyJson(@NotNull @JsonProperty("fromPath") String fromPath,
                    @NotNull @JsonProperty("targetPath") String targetPath) {
        this.fromPath = fromPath;
        this.targetPath = targetPath;
        this.compiledSourceJsonPath = JsonPath.compile(this.fromPath);
    }

    @Override
    public void apply(@NotNull JsonTransformerContext context) {
        List<ResolvedPaths> fromToMapping = context.queryAndResolveTargetPaths(compiledSourceJsonPath, targetPath);
        fromToMapping.forEach(mapping -> {
            Object sourceValue = context.queryForValue(mapping.getSourcePath());
            context.applyUpdate(mapping, sourceValue);
        });
    }
}
