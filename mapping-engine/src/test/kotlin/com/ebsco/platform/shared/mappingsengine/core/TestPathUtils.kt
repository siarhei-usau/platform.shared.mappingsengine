package com.ebsco.platform.shared.mappingsengine.core

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import org.junit.Test
import kotlin.test.assertEquals

class TestPathUtils {
    val json = """{ "a": { "b": [ { "c": { "d": { "e": "foo" } }, "something": "bar" }, { "c": { "d": { "e": "miss" } } } ] } }"""

    val jpathPaths = Configuration.builder()
            .options(Option.AS_PATH_LIST, Option.SUPPRESS_EXCEPTIONS)
            .jsonProvider(JacksonJsonProvider())
            .build()
            .let { JsonPath.using(it) }

    val jpathCtx = jpathPaths.parse(json)

    val jpathValues = Configuration.builder()
            .options(Option.SUPPRESS_EXCEPTIONS)
            .options(Option.ALWAYS_RETURN_LIST)
            .jsonProvider(JacksonJsonProvider())
            .build()

    @Test
    fun testAbsolutePathResolution() {
        val queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]"
        // common prefix will start at the point where the prefix of the searched path is in common

        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val insertionPoint = "$.a.b[*]+somethingNew"
        assertEquals(listOf(Triple("$['a']['b'][0]['c']['d']", "$['a']['b'][0]", "somethingNew")),
                jpathCtx.resolveTargetPaths(insertionPoint, matchingPaths))

        val insertionPointAtSameLevel = "$.a.b[*].c.d+e2"
        assertEquals(listOf(Triple("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", "e2")),
                jpathCtx.resolveTargetPaths(insertionPointAtSameLevel, matchingPaths))

        val replacementPoint = "$.a.b[*].c.d.e"
        assertEquals(listOf(Triple("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']['e']", "")),
                jpathCtx.resolveTargetPaths(replacementPoint, matchingPaths))
    }

    @Test
    fun testRelativePathResolutionDownwardOnly() {
        val queryPath = "$['a']['b'][*]"
        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val insertionPoint = "@.c.d+e"

        assertEquals(listOf(
                Triple("\$['a']['b'][0]", "\$['a']['b'][0]['c']['d']", "e"),
                Triple("\$['a']['b'][1]", "\$['a']['b'][1]['c']['d']", "e")),
                jpathCtx.resolveTargetPaths(insertionPoint, matchingPaths))

        val replacementPoint = "@.c.d.e"
        assertEquals(listOf(
                Triple("\$['a']['b'][0]", "\$['a']['b'][0]['c']['d']['e']", ""),
                Triple("\$['a']['b'][1]", "\$['a']['b'][1]['c']['d']['e']", "")),
                jpathCtx.resolveTargetPaths(replacementPoint, matchingPaths))

        val queryPath2 = "$.a.b[*].c.d[?(@.e == 'foo')]"
        val matchingPaths2: List<String> = jpathCtx.read(queryPath2)

        val replacementPoint2 = "@.e"
        assertEquals(listOf(Triple("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']['e']", "")),
                jpathCtx.resolveTargetPaths(replacementPoint2, matchingPaths2))

        val insertionPoint2 = "@+e2"
        assertEquals(listOf(Triple("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", "e2")),
                jpathCtx.resolveTargetPaths(insertionPoint2, matchingPaths2))
    }

    @Test
    fun testRelativePathResolutionUpAndDown() {
        val queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]"
        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val insertionPoint = "@^c^b+somethingNew"
        assertEquals(listOf(Triple("$['a']['b'][0]['c']['d']", "$['a']['b'][0]", "somethingNew")),
                jpathCtx.resolveTargetPaths(insertionPoint, matchingPaths))

        val insertionPointAtSameLevel = "@^c.d+e2"
        assertEquals(listOf(Triple("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", "e2")),
                jpathCtx.resolveTargetPaths(insertionPointAtSameLevel, matchingPaths))

        val replacementPoint = "@^c.d.e"
        assertEquals(listOf(Triple("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']['e']", "")),
                jpathCtx.resolveTargetPaths(replacementPoint, matchingPaths))

        // TODO not supporting deep bump up
        //  val target2 = "@^^b+somethingNew"   // traverse unknown steps up the tree until b is found within the found path

    }


    @Test
    fun testUpdates() {
        val queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]"
        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val insertionPoint = "@^c^b+somethingNew"
        val insertionInfo = jpathCtx.resolveTargetPaths(insertionPoint, matchingPaths)
        insertionInfo.forEach { (_, basePath, updatePath) ->
            jpathCtx.applyUpdatePath(basePath, updatePath, "HOWDY!")
        }

        val checkInsert: String = JsonPath.compile("$.a.b[0].somethingNew").read<List<String>>(jpathCtx.json<Any>(), jpathValues).first()
        assertEquals("HOWDY!", checkInsert)

        val insertionPointAtSameLevel = "@^c.d+e2"
        val insertionInfoAtSameLevel =  jpathCtx.resolveTargetPaths(insertionPointAtSameLevel, matchingPaths)
        insertionInfoAtSameLevel.forEach { (_, basePath, updatePath) ->
            jpathCtx.applyUpdatePath(basePath, updatePath, "HOWDY!")
        }

        val checkInsert2: String = JsonPath.compile("$.a.b[0].c.d.e2").read<List<String>>(jpathCtx.json<Any>(), jpathValues).first()
        assertEquals("HOWDY!", checkInsert2)


        val replacementPoint = "@^c.d.e"
        val replacementInfo = jpathCtx.resolveTargetPaths(replacementPoint, matchingPaths)
        replacementInfo.forEach { (_, basePath, updatePath) ->
            jpathCtx.applyUpdatePath(basePath, updatePath, "HOWDY!")
        }

        val checkInsert3: String = JsonPath.compile("$.a.b[0].c.d.e").read<List<String>>(jpathCtx.json<Any>(), jpathValues).first()
        assertEquals("HOWDY!", checkInsert3)

        val insertionPointNewArray = "@^c^b+newArray[+].value1"
        val insertionInfoNewArray = jpathCtx.resolveTargetPaths(insertionPointNewArray, matchingPaths)
        insertionInfoNewArray.forEach { (_, basePath, updatePath) ->
            jpathCtx.applyUpdatePath(basePath, updatePath, "HOWDY 1")
        }

        val insertionPointNewArray2 = "@^c^b+newArray[*].value2"
        val insertionInfoNewArray2 = jpathCtx.resolveTargetPaths(insertionPointNewArray2, matchingPaths)
        insertionInfoNewArray2.forEach { (_, basePath, updatePath) ->
            jpathCtx.applyUpdatePath(basePath, updatePath, "HOWDY 2")
        }
        val checkInsertInNewArray1: String = JsonPath.compile("$.a.b[0].newArray[0].value1").read<List<String>>(jpathCtx.json<Any>(), jpathValues).first()
        assertEquals("HOWDY 1", checkInsertInNewArray1)
        val checkInsertInNewArray2: String = JsonPath.compile("$.a.b[0].newArray[0].value2").read<List<String>>(jpathCtx.json<Any>(), jpathValues).first()
        assertEquals("HOWDY 2", checkInsertInNewArray2)
    }

}