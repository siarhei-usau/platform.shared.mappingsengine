package com.ebsco.platform.shared.mappingsengine.core.transformers;

import com.ebsco.platform.shared.mappingsengine.core.JsonTransformer;
import com.ebsco.platform.shared.mappingsengine.core.JsonTransformerContext;
import com.ebsco.platform.shared.mappingsengine.core.ResolvedPaths;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import io.burt.jmespath.Expression;
import io.burt.jmespath.JmesPath;
import io.burt.jmespath.jackson.JacksonRuntime;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MultiSelectJMESJson implements JsonTransformer {
    // The first thing you need is a runtime. These objects can compile expressions
// and they are specific to the kind of structure you want to search in.
// For most purposes you want the Jackson runtime, it can search in JsonNode
// structures created by Jackson.
    JmesPath<JsonNode> jmespath = new JacksonRuntime();

    @NonNull
    private String fromPath;

    @NonNull
    private String targetPath;

    @NonNull
    private String filterExpression;

    private Expression<JsonNode> jsonNodeExpression;

    private JsonPath compiledSourceJsonPath;

    private ObjectMapper mapper = new ObjectMapper();


    private ArrayNode arrayNode = mapper.createArrayNode();


    @JsonCreator
    public MultiSelectJMESJson(@NotNull @JsonProperty("fromPath") String fromPath,
                               @NotNull @JsonProperty("targetPath") String targetPath,
                               @NotNull @JsonProperty("filterExpression") String filterExpression) {
        this.fromPath = fromPath;
        this.targetPath = targetPath;
        this.compiledSourceJsonPath = JsonPath.compile(this.fromPath);
        this.filterExpression = filterExpression;
    }

    @Override
    public void apply(@NotNull JsonTransformerContext context) {
        Object sourceValue = context.queryForValue(fromPath);
        JsonNode input = mapper.convertValue(sourceValue, JsonNode.class);

        List<String> allMatches = new ArrayList<String>();
        Pattern pattern = Pattern.compile("\"[a-z-]+\"");
        Matcher matcher = pattern.matcher(filterExpression);
        while (matcher.find()) {
            allMatches.add(matcher.group().replace("\"", ""));
        }
        //check if any fields ara missing in the input, in order not to brake array structure
        for (int i = 0; i < input.size(); i++) {
            for (int j = 0; j < allMatches.size(); j++) {
                if (input.get(i).findValue(allMatches.get(j)) == null) {
                    ObjectNode jNode = ((ArrayNode) input.get(i)).addObject();
                    jNode.put(allMatches.get(j), "");
                }
            }
        }

        for (int i = 0; i < input.size(); i++) {
            if (i > 0) {
                filterExpression = filterExpression.replaceAll("\\d", String.valueOf(i));
            }
            // Expressions need to be compiled before you can search. Compiled expressions
            // are reusable and thread safe. Compile your expressions once, just like database
            // prepared statements.
            jsonNodeExpression = jmespath.compile(filterExpression);
            // Finally this is how you search a structure. There's really not much more to it.
            JsonNode result = jsonNodeExpression.search(input);
            arrayNode.add(result);
        }
        List<ResolvedPaths> fromToMapping = context.queryAndResolveTargetPaths(compiledSourceJsonPath, targetPath);
        fromToMapping.forEach(mapping -> {
            context.applyUpdate(mapping, arrayNode);
        });
    }
}
