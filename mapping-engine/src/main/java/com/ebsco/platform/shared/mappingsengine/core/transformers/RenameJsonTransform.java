package com.ebsco.platform.shared.mappingsengine.core.transformers;

import com.ebsco.platform.shared.mappingsengine.core.JsonTransformer;
import com.ebsco.platform.shared.mappingsengine.core.JsonTransformerContext;
import com.ebsco.platform.shared.mappingsengine.core.ResolvedPaths;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jayway.jsonpath.JsonPath;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.NotNull;

public class RenameJsonTransform implements JsonTransformer {
    @Getter
    private final String fromPath;

    @Getter
    private final String targetPath;

    @Getter
    private final JsonPath compiledSourceJsonPath;

    @JsonCreator
    public RenameJsonTransform(@NotNull @JsonProperty("fromPath") final String fromPath,
                               @NotNull @JsonProperty("targetPath") final String targetPath) {
        this.fromPath = fromPath;
        this.targetPath = targetPath;
        this.compiledSourceJsonPath = JsonPath.compile(this.fromPath);
    }

    @Override
    public void apply(@NotNull JsonTransformerContext context) {
        val fromToMapping = context.queryAndResolveTargetPaths(compiledSourceJsonPath, targetPath);

        for (ResolvedPaths mapping : fromToMapping) {
            context.applyUpdate(mapping, context.queryForValue(mapping.getSourcePath()));
            context.deleteValue(mapping.getSourcePath());
        }
    }
}
