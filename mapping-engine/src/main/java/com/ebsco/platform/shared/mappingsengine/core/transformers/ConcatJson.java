package com.ebsco.platform.shared.mappingsengine.core.transformers;

import com.ebsco.platform.shared.mappingsengine.core.JsonTransformer;
import com.ebsco.platform.shared.mappingsengine.core.JsonTransformerContext;
import com.ebsco.platform.shared.mappingsengine.core.ResolvedPaths;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

public class ConcatJson implements JsonTransformer {

    @NonNull
    private List<String> fromPaths;

    @NonNull
    private String targetPath;

    @NonNull
    private String delimiter;

    @JsonCreator
    public ConcatJson(@NotNull @JsonProperty("fromPaths") List<String> fromPaths,
                      @NotNull @JsonProperty("targetPath") String targetPath,
                      @NotNull @JsonProperty("delimiter") String delimiter) {
        this.fromPaths = fromPaths;
        this.targetPath = targetPath;
        this.delimiter = delimiter;
    }

    @Override
    public void apply(@NotNull JsonTransformerContext context) {
        Stream<FromPath2ResolvedTargetPathsList> mappings = fromPaths.stream()
                .map(fromPath -> FromPath2ResolvedTargetPathsList.of(fromPath,
                        context.queryAndResolveTargetPaths(fromPath, targetPath, true)));

        Map<TargetBasePath2TargetUpdatePath, List<Mapping>> groupedMappings =
                mappings.flatMap(fromPath2ResolvedTargetPathsList -> fromPath2ResolvedTargetPathsList.getTargetPaths().stream()
                        .map(it -> {
                            TargetBasePath2TargetUpdatePath key = TargetBasePath2TargetUpdatePath.of(
                                    it.getTargetBasePath(),
                                    it.getTargetUpdatePath()
                            );
                            return Mapping.of(key, FromPath2ResolvedTargetPaths.of(
                                    fromPath2ResolvedTargetPathsList.getFromPath(), it));

                        })).collect(groupingBy(Mapping::getFrom));

        Map<TargetBasePath2TargetUpdatePath, Map<String, String>> mappingsGroupedByTarget =
                groupedMappings.entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey,
                                e -> e.getValue().stream()
                                        .map(Mapping::getTo)
                                        .collect(toMap(
                                                FromPath2ResolvedTargetPaths::getFromPath,
                                                it -> it.getResolvedTargetPaths().getSourcePath()))));

        // we now have a map of target path, to a map of source paths to actual value path (in the resolved object)

        // for each target path, collect the values in the order of the original source path list
        mappingsGroupedByTarget.forEach((targetPath, sourceMap) -> {
            List<Object> sourceValuesInOrder = fromPaths.stream()
                    .map(sourceMap::get)
                    .filter(Objects::nonNull)
                    .map(context::queryForValues)
                    .filter(Objects::nonNull)
                    .filter(l -> !l.isEmpty())
                    .collect(toList());
            String jsonFragment = sourceValuesInOrder.stream()
                    .flatMap(l -> ((List<Object>)l).stream())
                    .map(o -> o.toString())
                    .collect(joining(delimiter));
            context.applyUpdate(targetPath.getTargetBasePath(), targetPath.getTargetUpdatePath(),
                    jsonFragment);
        });

    }

    @Value(staticConstructor = "of")
    private static class FromPath2ResolvedTargetPathsList {
        String fromPath;
        List<ResolvedPaths> targetPaths;
    }

    @Value(staticConstructor = "of")
    private static class TargetBasePath2TargetUpdatePath {
        String targetBasePath;
        String targetUpdatePath;
    }

    @Value(staticConstructor = "of")
    private static class Mapping {
        TargetBasePath2TargetUpdatePath from;
        FromPath2ResolvedTargetPaths to;
    }

    @Value(staticConstructor = "of")
    private static class FromPath2ResolvedTargetPaths {
        String fromPath;
        ResolvedPaths resolvedTargetPaths;
    }
}
