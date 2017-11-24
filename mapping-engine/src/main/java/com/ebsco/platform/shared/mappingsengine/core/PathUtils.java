package com.ebsco.platform.shared.mappingsengine.core;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JsonProvider;
import lombok.Value;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.ebsco.platform.shared.mappingsengine.core.StringUtils.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

// TODO: exception messages

@UtilityClass
public class PathUtils {

    // TODO: clean up path parsing with more formal parser than string splits
    //  then stay with parsed paths until the very end, and render
    // This will prevent going to from string paths a few times in some cases

    public List<ResolvedPaths> resolveTargetPaths(DocumentContext documentContext,
                                                  String targetPathString,
                                                  List<String> matchingPaths,
                                                  boolean allowNoMatchingTarget) {

        Configuration config = buildConfig(documentContext, Option.AS_PATH_LIST);
        Object json = documentContext.json();

        TargetPath targetPath = TargetPath.fromString(targetPathString);
        BasePath basePath = targetPath.getBasePath();

        List<Match> matches = new ArrayList<>();
        if (basePath.isAbsolute()) {
            JsonPath compiledBasePath = basePath.compile();
            List<String> foundPaths = compiledBasePath.read(json, config);

            // compare found nodes against matching source nodes, we need a match for each of them
            matches = matchPaths(matchingPaths, foundPaths);
        } else if (basePath.isRelative()) {
            if (basePath.getPathString().equals("@")) {
                // special case of @+foo would result in only @ as existing path
                matches = matchingPaths.stream()
                        .map(path -> Match.of(path, path))
                        .collect(toList());
            } else if (basePath.hasUpwardParts()) {

                List<String> upwardsParts = basePath.getUpwardParts();
                String downwardsPart = basePath.getDownWardPart();

                // go up for each path for each upward part
                List<Match> uppedPaths = matchingPaths.stream().map(matchingPath -> {

                    String temp = matchingPath.substring(1); // skip the $
                    List<String> tempParts = Arrays.stream(temp.split("]"))
                            .map(it -> removePrefix(it, "["))
                            .filter(it -> !it.trim().isEmpty())
                            .collect(toList());

                    String lastPath = null;
                    for (String upper : upwardsParts) {
                        // going up from a.b[1].c goes to b[1],
                        //         is ['a']['b'][1]['c'] to ['a']['b'][1]
                        // going up from a.b[1] goes to a,
                        //         is ['a']['b'][1] to ['a']
                        // going up from a.b[1].c[1] goes to b[1]
                        //         is ['a']['b'][1]['c'][1] to ['a']['b'][1]
                        if (tempParts.size() < 2) {
                            throw new IllegalStateException("Cannot path upwards using "+targetPath+" from starting " + matchingPath + ", attempted to pop up past the first element");
                        }
                        String inspectPossibleArrayIndex = tempParts.get(tempParts.size() - 1);
                        // we are on an array index, so from perspective of popping up, start at the array
                        if (isNumber(inspectPossibleArrayIndex)) {
                            tempParts.remove(tempParts.size() - 1);
                        }
                        // pop up to the possible landing point
                        tempParts.remove(tempParts.size() - 1);
                        lastPath = tempParts.stream()
                                .map(it -> "[" + it + "]")
                                .collect(joining("", "$", ""));

                        // the ID of the landing point is this node if it is not an array index, otherwise is the node above
                        if (tempParts.isEmpty()) {
                            throw new IllegalStateException("Cannot path upwards using "+targetPath+" from starting " + matchingPath + ", attempted to pop up past the first element");
                        }
                        String inspectLastPartAgain = tempParts.get(tempParts.size() - 1);
                        String checkPoint;
                        if (isNumber(inspectLastPartAgain)) {
                            if (tempParts.size() < 2) {
                                throw new IllegalStateException("Cannot path upwards using "+targetPath+" from starting " + matchingPath + ", unexpected array index as first element");
                            }
                            checkPoint = tempParts.get(tempParts.size() - 2);
                        } else {
                            checkPoint = inspectLastPartAgain;
                        }

                        if (checkPoint.startsWith("'")) {
                            String id = strip(checkPoint, "'");
                            if (!id.equals(upper)) {
                                throw new IllegalStateException("Cannot path upwards using "+targetPath+" from starting "+matchingPath+", error popping up to "+upper+", found "+id+" instead");
                            }
                        } else {
                            throw new IllegalStateException("Cannot path upwards using "+targetPath+" from starting " + matchingPath + ", the next part has no valid name");
                        }
                    }
                    return Match.of(matchingPath, lastPath);
                }).collect(toList());

                if (downwardsPart.trim().isEmpty()) {
                    matches = uppedPaths;
                } else {
                    matches = uppedPaths.stream()
                            .map(upped -> Match.of(upped.getPath(), upped.getPrefix() + "." + downwardsPart))
                            .flatMap(updated -> {
                                String matchingPath = updated.getPath();
                                String testPath = updated.getPrefix();
                                List<String> foundPaths = JsonPath.compile(testPath).read(json, config);

                                if (foundPaths.isEmpty()) {
                                    return Stream.of(Match.of(matchingPath, null));
                                } else {
                                    return foundPaths.stream().map(foundPath -> Match.of(matchingPath, foundPath));
                                }
                            }).collect(toList());
                }
            } else {
                // current node and down
                matches = matchingPaths.stream()
                        .map(matchingPath -> Match.of(matchingPath, matchingPath + basePath.getPathString().substring(1)))
                        .flatMap(updated -> {
                            String matchingPath = updated.getPath();
                            String testPath = updated.getPrefix();
                            List<String> foundPaths = JsonPath.compile(testPath).read(json, config);

                            if (foundPaths.isEmpty()) {
                                return Stream.of(Match.of(matchingPath, null));
                            } else {
                                return foundPaths.stream().map(foundPath -> Match.of(matchingPath, foundPath));
                            }
                        }).collect(toList());
            }
        }

        List<Match> matchingPathsMissing = matches.stream()
                .filter(match -> match.getPrefix() == null)
                .collect(toList());
        if (!allowNoMatchingTarget && !matchingPathsMissing.isEmpty()) {
            throw new IllegalStateException("Some source paths cannot be related to a root existing path: " + matchingPathsMissing);
        }

        List<ResolvedPaths> normalizedPaths = matches.stream().map(match -> {
            String originalTarget = match.getPrefix() != null ? match.getPrefix() : "";
            String normalizedTarget;
            if (targetPath.getUpdatePath().startsWith("[") && !originalTarget.trim().isEmpty()) {
                // we are possibly changing an array index in place
                List<String> tempParts = Arrays.stream(originalTarget.substring(1).split(Pattern.quote("]")))
                        .map(it -> removePrefix(it, "["))
                        .filter(it -> !it.trim().isEmpty())
                        .collect(toList());
                String checkLastPart = tempParts.get(tempParts.size() - 1);
                List<String> finalParts;
                if (isNumber(checkLastPart)) {
                    tempParts.remove(tempParts.size() - 1);
                }
                finalParts = tempParts;
                normalizedTarget = finalParts.stream()
                        .map(it -> "[" + it + "]")
                        .collect(joining("", "$", ""));

            } else {
                normalizedTarget = originalTarget;
            }
            return ResolvedPaths.of(match.getPath(), normalizedTarget, targetPath.getUpdatePath());
        }).collect(toList());

        return normalizedPaths;
    }

    public List<ResolvedPaths> resolveTargetPaths(DocumentContext documentContext,
                                                  String targetPathString,
                                                  List<String> matchingPaths
                                                  ) {
        return resolveTargetPaths(documentContext, targetPathString, matchingPaths, false);
    }

    public void applyUpdatePath(DocumentContext documentContext, String basePath, String updatePath, Object jsonFragment) {
        Configuration config = buildConfig(documentContext, Option.ALWAYS_RETURN_LIST);
        JsonPath startingPath = JsonPath.compile(basePath);
        Object json = documentContext.json();
        JsonProvider jsonProvider = config.jsonProvider();

        if (updatePath.trim().isEmpty()) {
            startingPath.set(json, jsonFragment, config);
        } else {
            List<Object> read = startingPath.read(json, config);
            if (read.isEmpty()) {
                throw new IllegalStateException("Base path for update " + basePath + " was not found in document");
            }
            Object baseNode = read.get(0);
            List<String> updateSteps = Arrays.asList(updatePath.trim().split(Pattern.quote(".")));

            drillDownToUpdate(baseNode, updatePath, updateSteps, jsonFragment, jsonProvider);
        }
    }

    private void drillDownToUpdate(Object startNode, String updatePath, List<String> steps, Object jsonFragment, JsonProvider provider) {
        Object currentNode = startNode;
        List<String> currentSteps = steps;

        while (!currentSteps.isEmpty()) {
            String step = currentSteps.get(0);
            boolean isLastStep = currentSteps.size() == 1;
            currentSteps = currentSteps.subList(1, currentSteps.size());

            String id = substringBefore(step, "[", step);
            String idx = removeSuffix(substringAfter(step, "[", ""), "]");
            boolean silentArrayIndex = id.trim().isEmpty() && !idx.trim().isEmpty();

            if (silentArrayIndex && !provider.isArray(currentNode)) {
                throw new IllegalStateException("Update pathing through [" + idx + "] found something other than an Array at current point");
            }
            if (!silentArrayIndex && !provider.isMap(currentNode)) {
                throw new IllegalStateException("Update pathing through " + id + "found something other than a Map at current point");
            }

            if (idx.trim().isEmpty()) {
                //map or property
                if (isLastStep) {
                    provider.setProperty(currentNode, id, jsonFragment);
                } else {
                    if (!provider.getPropertyKeys(currentNode).contains(id)) {
                        Object newNode = provider.createMap();
                        provider.setProperty(currentNode, id, newNode);
                        currentNode = newNode;
                    } else {
                        currentNode = provider.getMapValue(currentNode, id);
                    }
                }

            } else {
                Object checkArrayNode = null;
                if (silentArrayIndex) {
                    checkArrayNode = currentNode;
                } else if (provider.getPropertyKeys(currentNode).contains(id)) {
                    checkArrayNode = provider.getMapValue(currentNode, id);
                }
                if ((checkArrayNode == null && (idx.equals("*") || idx.equals("0"))) ||
                        (checkArrayNode != null && !provider.isArray(checkArrayNode))) {
                    throw new IllegalStateException("Expected array at "+id+" in "+startNode+" / "+updatePath+" during update traversal");
                }
                Object arrayNode;
                if (checkArrayNode != null) {
                    arrayNode = checkArrayNode;
                } else {
                    Object array = provider.createArray();
                    provider.setProperty(currentNode, id, array);
                    arrayNode = array;
                }

                int arraySize = provider.length(arrayNode);

                if (idx.equals("*") || idx.equals("*+")) {
                    for (int i = 0; i < arraySize; ++i) {
                        if (isLastStep) {
                            provider.setArrayIndex(arrayNode, i, jsonFragment);
                        } else {
                            drillDownToUpdate(provider.getArrayIndex(arrayNode, i), updatePath, currentSteps, jsonFragment, provider);
                        }
                    }

                    if (arraySize == 0 && idx.endsWith("+")) {
                        if (isLastStep) {
                            provider.setArrayIndex(arrayNode, arraySize, jsonFragment);
                        } else {
                            Object emptyItem = provider.createMap();
                            provider.setArrayIndex(arrayNode, arraySize, emptyItem);
                            drillDownToUpdate(emptyItem, updatePath, currentSteps, jsonFragment, provider);
                            return;
                        }
                    }
                } else if (idx.equals("0") || idx.equals("0+")) {
                    if (isLastStep) {
                        provider.setArrayIndex(arrayNode, 0, jsonFragment );
                    } else {
                        drillDownToUpdate(provider.getArrayIndex(arrayNode, 0), updatePath, steps, jsonFragment, provider);
                        return;
                    }

                    if (arraySize == 0 && idx.endsWith("+")) {
                        if (isLastStep) {
                            provider.setArrayIndex(arrayNode, arraySize, jsonFragment);
                        } else {
                            Object emptyItem = provider.createMap();
                            provider.setArrayIndex(arrayNode, arraySize, emptyItem);
                            drillDownToUpdate(emptyItem, updatePath, currentSteps, jsonFragment, provider);
                            return;
                        }
                    }
                } else if (idx.equals("+")) {
                    if (isLastStep) {
                        provider.setArrayIndex(arrayNode, arraySize, jsonFragment);
                    } else {
                        Object emptyItem = provider.createMap();
                        provider.setArrayIndex(arrayNode, arraySize, emptyItem);
                        drillDownToUpdate(emptyItem, updatePath, currentSteps, jsonFragment, provider);
                        return;
                    }
                } else {
                    throw new IllegalStateException("Update pathing contains invalid array modifier, " + startNode +" / "+updatePath);
                }
            }
        }
    }

    private Configuration buildConfig(DocumentContext documentContext, Option option) {
        return Configuration.builder()
                .options(option, Option.SUPPRESS_EXCEPTIONS)
                .jsonProvider(documentContext.configuration().jsonProvider())
                .mappingProvider(documentContext.configuration().mappingProvider())
                .build();
    }

    private List<Match> matchPaths(List<String> matchingPaths, List<String> foundPaths) {
        return matchingPaths.stream()
                .flatMap(matchingPath -> foundPaths.stream()
                        .filter(foundPath -> matchingPath.startsWith(foundPath) || foundPath.startsWith(matchingPath))
                        .map(prefix -> Match.of(matchingPath, prefix)))
                .collect(toList());
    }


    @Value(staticConstructor = "of")
    private class Match {
        String path;
        String prefix;
    }

    /**
     * two types of paths:
     * <p>
     * absolute:   $.a.b.c[*].d[0].e[?(@.filter)].f
     * relative:   @^e^d.z
     * relative:   @.x.y.z
     * <p>
     * both can have one update marker '+' in the path indicating that the path to the left must exist, and the
     * remainder of the path to the right may be inserted.
     * <p>
     * absolute:   $a.b.c[*]+d.e.f
     * relative:   @^y^x+d[0+].e.f
     * relative:   @.x+y.z
     */
    @Value(staticConstructor = "of")
    private class TargetPath {
        BasePath basePath;
        String updatePath;

        static TargetPath fromString(String targetPath) {
            String cleanPath = targetPath.trim();
            String basePath = substringBefore(cleanPath, "+", cleanPath);
            String updatePath = substringAfter(cleanPath, "+", "");
            return TargetPath.of(BasePath.fromString(basePath), updatePath);
        }
    }

    @Value(staticConstructor = "of")
    private class BasePath {
        String pathString;

        static BasePath fromString(String basePath) {
            String cleanPath = basePath.trim();
            if (cleanPath.isEmpty()) {
                throw new IllegalStateException("A base path cannot be blank");
            }
            if (!cleanPath.startsWith("$") && !cleanPath.startsWith("@")) {
                throw new IllegalArgumentException("Base path must start with $ absolute, or @ relative path marker " + basePath);
            }
            if (cleanPath.contains("^")) {
                int firstDotPosition = cleanPath.indexOf('.');
                if (cleanPath.charAt(1) != '^' ||
                        (firstDotPosition != -1 && cleanPath.indexOf('^', firstDotPosition) != -1)) {
                    throw new IllegalStateException("Relative path " + basePath + " must have ^ as second character and all ^ before the first .");
                }
            } else {
                if (basePath.length() > 1 && (basePath.charAt(1) != '.' && basePath.charAt(1) != '[')) {
                    throw new IllegalStateException("Expected '.' or '[' as second char in @ relative path, " + basePath);
                }
            }
            return BasePath.of(cleanPath);
        }

        boolean hasUpwardParts() {
            return pathString.contains("^");
        }

        boolean isAbsolute() {
            return pathString.startsWith("$");
        }

        boolean isRelative() {
            return pathString.startsWith("@");
        }

        List<String> getUpwardParts() {
            // after popping up, we can only go downwards.  So start with all the upward movement...
            String startingPath = pathString.substring(2);

            // and now . or [] starts downwards again...
            int earliestDownSymbol = indexOfAny(startingPath, '.', '[');
            String upwards = earliestDownSymbol >= 0 ? startingPath.substring(0, earliestDownSymbol) : startingPath;
            return Arrays.asList(upwards.split(Pattern.quote("^")));
        }

        String getDownWardPart() {
            String startingPath = pathString.substring(2);

            int earliestDownSymbol = indexOfAny(startingPath, '.', '[');
            return earliestDownSymbol >= 0 ?
                    removePrefix(startingPath.substring(earliestDownSymbol), ".") : "";

        }

        JsonPath compile() {
            return JsonPath.compile(pathString);
        }
    }

}
