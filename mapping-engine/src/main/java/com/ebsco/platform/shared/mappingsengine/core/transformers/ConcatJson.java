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
        // it gets complicated to handle cases where the source values are not in the same objects.  So we assume they are.
        //
        // Therefore we expect for every sourceJsonPath a list of matching paths of which some have target paths (if a value was present)
        //
        //
        // So to handle the city/state case where state is at a level above cities and you want to build "city, state", you would do multiple transforms:
        //     copy the state name into the city object
        //     concat the city and state within the same object
        //     remove the temp state field

        // For mixed object selection we can look at doing prefix matching to find the closest match from each when selecting from multiple objects
        // and allow mixed object selection:
        //
        //     $.state[0].cities[0].name  "Denver"            target match $.state[0].cities[0]+cityState
        //     $.state[0].cities[1].name  "Boulder"           target match $.state[0].cities[1]+cityState
        //     $.state[0].name            "Colorado"          none, but path $.state[0] is prefix match for above
        //     $.state[1].cities[0].name  "San Francisco"     target match $.state[1].cities[0]+cityState
        //     $.state[1].cities[1].name  "Santa Cruz"        target match $.state[1].cities[1]+cityState
        //     $.state[1].name            "California"        none, but path $.state[1] is a prefix match for above
        //

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
