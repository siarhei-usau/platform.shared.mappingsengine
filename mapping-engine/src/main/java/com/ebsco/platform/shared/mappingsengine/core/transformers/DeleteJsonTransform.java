package com.ebsco.platform.shared.mappingsengine.core.transformers;

import com.ebsco.platform.shared.mappingsengine.core.JsonTransformer;
import com.ebsco.platform.shared.mappingsengine.core.JsonTransformerContext;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jayway.jsonpath.JsonPath;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.NotNull;

public class DeleteJsonTransform implements JsonTransformer {
    @Getter
    private final String deletePath;

    @Getter
    private final JsonPath compiledSourceJsonPath;

    @JsonCreator
    public DeleteJsonTransform(@NotNull @JsonProperty("deletePath") final String deletePath) {
        this.deletePath = deletePath;
        this.compiledSourceJsonPath = JsonPath.compile(this.deletePath);
    }

    @Override
    public void apply(@NotNull JsonTransformerContext context) {
        val pathList = context.queryForPaths(compiledSourceJsonPath);
        for (String path : pathList) {
            context.deleteValue(path);
        }
    }
}

