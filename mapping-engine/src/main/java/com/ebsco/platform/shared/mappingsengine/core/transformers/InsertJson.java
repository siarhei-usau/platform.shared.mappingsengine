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

public class InsertJson implements JsonTransformer {

    @NonNull
    private String testPath;

    @NonNull
    private String targetPath;

    @NonNull
    private Object jsonFragment;

    private JsonPath compiledSourceJsonPath;

    @JsonCreator
    public InsertJson(@NotNull @JsonProperty("testPath") String testPath, @NotNull @JsonProperty("targetPath") String targetPath, @NotNull @JsonProperty("jsonFragment") Object jsonFragment) {
        this.testPath = testPath;
        this.targetPath = targetPath;
        this.jsonFragment = jsonFragment;
        this.compiledSourceJsonPath = JsonPath.compile(this.testPath);
    }

    @Override
    public void apply(@NotNull JsonTransformerContext context) {
        List<ResolvedPaths> fromToMapping = context.queryAndResolveTargetPaths(compiledSourceJsonPath, targetPath);
        fromToMapping.forEach(mapping -> {
            context.applyUpdate(mapping, jsonFragment);
        });
    }
}
