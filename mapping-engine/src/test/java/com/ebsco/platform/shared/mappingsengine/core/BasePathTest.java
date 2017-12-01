package com.ebsco.platform.shared.mappingsengine.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;

public abstract class BasePathTest {
    protected static final String json = "{\n" +
            "            \"a\": {\n" +
            "                \"b\": [{\n" +
            "                    \"c\": {\n" +
            "                        \"d\": {\n" +
            "                            \"e\": \"foo\"\n" +
            "                        }\n" +
            "                    },\n" +
            "                    \"something\": \"bar\"\n" +
            "                }, {\n" +
            "                    \"c\": {\n" +
            "                        \"d\": {\n" +
            "                            \"e\": \"miss\"\n" +
            "                        }\n" +
            "                    }\n" +
            "                }]\n" +
            "            },\n" +
            "            \"states\": [\n" +
            "                {\n" +
            "                    \"name\": \"Colorado\",\n" +
            "                    \"cities\": [\n" +
            "                        {\n" +
            "                           \"geo\": \"39.726287, −104.965486\",\n" +
            "                           \"name\":\"Denver\"\n" +
            "                        },\n" +
            "                        {\n" +
            "                           \"geo\": \"40.014984, -105.270546\",\n" +
            "                           \"name\": \"Boulder\"\n" +
            "                        }\n" +
            "                    ]\n" +
            "                },\n" +
            "                {\n" +
            "                    \"name\": \"California\",\n" +
            "                    \"cities\": [\n" +
            "                        {\n" +
            "                           \"geo\": \"37.7749300, -122.4194200\",\n" +
            "                           \"name\":\"San Francisco\"\n" +
            "                        },\n" +
            "                        {\n" +
            "                           \"geo\": \"36.974120, -122.030800\",\n" +
            "                           \"name\": \"Santa Cruz\"\n" +
            "                        }\n" +
            "                    ]\n" +
            "                }\n" +
            "            ],\n" +
            "            \"people\": [\n" +
            "                {\n" +
            "                   \"firstname\": \"David\",\n" +
            "                   \"lastname\": \"Smith\"\n" +
            "                },\n" +
            "                {\n" +
            "                   \"firstname\": \"Michael\",\n" +
            "                   \"lastname\": \"Stark\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"issue19\": [\n"+
            "                [ { \"one\": \"value1A\" },{ \"two\": \"value2A\" } ],\n"+
            "                [ { \"one\": \"value1B\" },{ \"two\": \"value2B\" } ],\n"+
            "                [ { \"one\": \"value1C\" },{ \"two\": \"value2C\" } ]\n"+
            "            ],\"contrib\": [\n" +
            "  [{\"contrib-type\": \"author\"},{\"surname\": \"Chen\"},\n" +
            "\t{\"contrib-id-string\": \"38787138\"}],\n" +
            "  [{\"rid\": 2},{\"contrib-type\": \"author\"},\n" +
            "    {\"given-names\": \"Patrick Mark\"}\n" +
            "    ,{\"surname\": \"Singh\"}]\n" +
            "    ]\n"+
            "        }";

    protected Configuration jpathConfig = Configuration.builder()
            .options(Option.AS_PATH_LIST, com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS)
            .jsonProvider(new JacksonJsonProvider())
            .build();

    protected ParseContext jpathPaths = JsonPath.using(jpathConfig);

    protected DocumentContext jpathCtx = jpathPaths.parse(json);

    protected Configuration jvalueListConfig = Configuration.builder()
            .options(Option.SUPPRESS_EXCEPTIONS)
            .options(Option.ALWAYS_RETURN_LIST)
            .jsonProvider(new JacksonJsonProvider())
            .build();

    protected Configuration jvalueConfig = Configuration.builder()
            .options(Option.SUPPRESS_EXCEPTIONS)
            .jsonProvider(new JacksonJsonProvider())
            .build();

    protected void printJson(Object tree, String title) throws JsonProcessingException {
        String s = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(tree);
        System.out.println(s);
        System.out.flush();
    }

    protected JsonTransformerContext makeContext() {
        return new JsonTransformerContext(jpathCtx.json(), jpathConfig, jvalueConfig, jvalueListConfig);
    }
}
