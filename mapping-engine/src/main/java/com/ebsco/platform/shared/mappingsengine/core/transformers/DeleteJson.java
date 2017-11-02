package com.ebsco.platform.shared.mappingsengine.core.transformers;

import com.ebsco.platform.shared.mappingsengine.core.JsonTransformer;
import com.ebsco.platform.shared.mappingsengine.core.JsonTransformerContext;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jayway.jsonpath.JsonPath;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class DeleteJson implements JsonTransformer {

    @NonNull
    private String deletePath;

    private JsonPath compiledSourceJsonPath;

    @JsonCreator
    public DeleteJson(@NotNull @JsonProperty("deletePath") String deletePath) {
        this.deletePath = deletePath;
        this.compiledSourceJsonPath = JsonPath.compile(this.deletePath);
    }

    @Override
    public void apply(@NotNull JsonTransformerContext context) {
        context.queryForPaths(compiledSourceJsonPath)
                .forEach(context::deleteValue);
    }
}
