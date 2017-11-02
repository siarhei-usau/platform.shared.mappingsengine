package com.ebsco.platform.shared.mappingsengine.core.transformers;

import com.ebsco.platform.shared.mappingsengine.core.JsonTransformer;
import com.ebsco.platform.shared.mappingsengine.core.JsonTransformerContext;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LookupJson implements JsonTransformer {
    @NonNull
    private String lookupResource;

    @NonNull
    private List<LookupFilter> filters = Collections.emptyList();

    @NonNull
    private LookupApplyModes mode = LookupApplyModes.merge;

    @NonNull
    private String targetPath;

    @NonNull
    private Map<String, Object> jsonTemplate;

    @JsonCreator
    public LookupJson(@NotNull @JsonProperty("lookupResource") String lookupResource,
                      @NotNull @JsonProperty("filters") List<LookupFilter> filters,
                      @NotNull @JsonProperty("mode") LookupApplyModes mode,
                      @NotNull @JsonProperty("targetPath") String targetPath,
                      @NotNull @JsonProperty("jsonTemplate") Map<String, Object> jsonTemplate) {
        this.lookupResource = lookupResource;
        this.filters = filters;
        this.mode = mode;
        this.targetPath = targetPath;
        this.jsonTemplate = jsonTemplate;
    }

    @Override
    public void apply(@NotNull JsonTransformerContext context) {
        // TODO, no Kotlin implementation
    }

    enum LookupApplyModes {
        merge, insert, replace
    }

    static class LookupFilter {
        @NonNull
        String lookupField;

        @Nullable
        String fromPath;

        @NonNull
        List<String> lookupValues = Collections.emptyList();

        public LookupFilter(@NotNull String lookupField, @NotNull String fromPath) {
            this.lookupField = lookupField;
            this.fromPath = fromPath;
            this.lookupValues = Collections.emptyList();
        }

        public LookupFilter(@NotNull String lookupField, @NotNull List<String> lookupValues) {
            this.lookupField = lookupField;
            this.fromPath = null;
            this.lookupValues = lookupValues;
        }

        @JsonCreator
        public LookupFilter(@NotNull @JsonProperty("lookupField") String lookupField,
                            @JsonProperty("fromPath") String fromPath,
                            @JsonProperty("lookupValues") List<String> lookupValues) {
            this.lookupField = lookupField;

            if (fromPath != null && lookupValues != null && !lookupValues.isEmpty()) {
                throw new IllegalArgumentException("Cannot have both a fromPath and lookupValues, it is either/or");
            }

            this.fromPath = fromPath;
            this.lookupValues = lookupValues;
        }
    }
}



