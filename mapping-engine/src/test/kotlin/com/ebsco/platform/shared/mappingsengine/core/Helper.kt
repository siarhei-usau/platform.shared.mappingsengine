package com.ebsco.platform.shared.mappingsengine.core

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
                           "geo": "39.726287, −104.965486",
                           "name":"Denver"
                        },
                        {
                           "geo": "40.014984, -105.270546",
                           "name": "Boulder"
                        }
                    ]
                },
                {
                    "name": "California",
                    "cities": [
                        {
                           "geo": "37.7749300, -122.4194200",
                           "name":"San Francisco"
                        },
                        {
                           "geo": "36.974120, -122.030800",
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
    protected val jpathPaths = JsonPath.using(jpathConfig)

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