package com.ebsco.platform.shared.mappingsengine.core

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonProvider

abstract class BasePathTest {
    protected val json = """
        {
            "a": {
                "b": [{
                    "c": {
                        "d": {
                            "e": "foo"
                        }
                    },
                    "something": "bar"
                }, {
                    "c": {
                        "d": {
                            "e": "miss"
                        }
                    }
                }]
            },
            "states": [
                {
                    "name": "Colorado",
                    "cities": [
                        {
                           "geo": "x,y",
                           "name":"Denver"
                        },
                        {
                           "geo": "x,y",
                           "name": "Boulder"
                        }
                    ]
                },
                {
                    "name": "California",
                    "cities": [
                        {
                           "geo": "x,y",
                           "name":"San Francisco"
                        },
                        {
                           "geo": "x,y",
                           "name": "Santa Cruz"
                        }
                    ]
                }
            ],
            "people": [
                {
                   "firstname": "David",
                   "lastname": "Smith"
                },
                {
                   "firstname": "Michael",
                   "lastname": "Stark"
                }
            ]
        }
    """.trimIndent()

    protected val jpathConfig = Configuration.builder()
            .options(Option.AS_PATH_LIST, Option.SUPPRESS_EXCEPTIONS)
            .jsonProvider(JacksonJsonProvider())
            .build()
    protected val jpathPaths =  JsonPath.using(jpathConfig)

    protected val jpathCtx = jpathPaths.parse(json)

    protected val jvalueListConfig = Configuration.builder()
            .options(Option.SUPPRESS_EXCEPTIONS)
            .options(Option.ALWAYS_RETURN_LIST)
            .jsonProvider(JacksonJsonProvider())
            .build()

    protected val jvalueConfig = Configuration.builder()
            .options(Option.SUPPRESS_EXCEPTIONS)
            .jsonProvider(JacksonJsonProvider())
            .build()

    protected fun printJson(tree: Any, title: String = "Pretty JSON") {
        println()
        println(title)
        val prettyJson = ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(tree)
        println(prettyJson)
    }
}