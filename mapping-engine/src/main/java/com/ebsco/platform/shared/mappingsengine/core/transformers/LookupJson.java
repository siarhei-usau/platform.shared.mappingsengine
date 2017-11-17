package com.ebsco.platform.shared.mappingsengine.core.transformers;

import com.ebsco.platform.shared.mappingsengine.core.JsonTransformer;
import com.ebsco.platform.shared.mappingsengine.core.JsonTransformerContext;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

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

    @NotNull
    @JsonIgnore
    private List<Map<String, String>> lookupRecords;

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

        try {
            InputStream lookupContents = null;
            if (this.lookupResource.startsWith("file:")) {
                String filename = this.lookupResource.substring(5);
                lookupContents = new FileInputStream(new File(filename));
            } else if (this.lookupResource.startsWith("classpath:")) {
                String filename = this.lookupResource.substring(10);
                lookupContents = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
                if (lookupContents == null) {
                    lookupContents = ClassLoader.getSystemResourceAsStream(filename);
                }
                if (lookupContents == null) {
                    lookupContents = getClass().getResourceAsStream(filename);
                }
                if (lookupContents == null) {
                    throw new IllegalStateException("Lookup resource not found in classpath: " + this.lookupResource);
                }
            } else {
                throw new IllegalStateException("Unexpected protocol for lookup file (expected one of ['file', 'classpath']).");
            }

            if (this.lookupResource.endsWith(".gz")) {
                lookupContents = new GZIPInputStream(lookupContents);
            }

            // TODO: the input data is not correctly encoded, and seems to be malformed
            Reader contentsReader = new InputStreamReader(lookupContents, "UTF-8");

            CsvMapper mapper = new CsvMapper();
            CsvSchema schema = CsvSchema.emptySchema().withColumnSeparator('\t').withHeader();
            MappingIterator<Map<String,String>> it = mapper.readerFor(Map.class)
                    .with(schema)
                    .readValues(contentsReader);
            lookupRecords = new ArrayList<>();
            while (it.hasNext()) {
                try {
                    Map<String, String> rowAsMap = it.next();
                    lookupRecords.add(rowAsMap);
                } catch (RuntimeException ex) {
                    // nop, we have bad encoding, bad rows, skip those
                    // TODO: the input data is not correctly encoded, and seems to be malformed
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Error reading lookup file: " + ex.getMessage());
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Error reading lookup file: " + ex.getMessage());
        }
    }

    @Override
    public void apply(@NotNull JsonTransformerContext context) {
        // TODO, no Kotlin implementation
    }

    public enum LookupApplyModes {
        merge, insert, replace
    }

    public static class LookupFilter {
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



