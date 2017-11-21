package com.ebsco.platform.shared.mappingsengine.core

import org.junit.Test
import kotlin.test.assertEquals
import com.ebsco.platform.shared.mappingsengine.core.PathUtils.applyUpdatePath
import com.ebsco.platform.shared.mappingsengine.core.PathUtils.resolveTargetPaths
import com.jayway.jsonpath.JsonPath
import org.junit.Ignore
import kotlin.test.fail

// validating that the original kotlin test cases pass for the Java code
class OriginalPathUtilsTest : OriginalBasePathTest() {

    @Test
    fun testAbsolutePathResolution() {
        val queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]"
        // common prefix will start at the point where the prefix of the searched path is in common

        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val insertionPoint = "$.a.b[*]+somethingNew"
        assertEquals(listOf(ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]", "somethingNew")),
                resolveTargetPaths(jpathCtx, insertionPoint, matchingPaths))

        val insertionPointAtSameLevel = "$.a.b[*].c.d+e2"
        assertEquals(listOf(ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", "e2")),
                resolveTargetPaths(jpathCtx, insertionPointAtSameLevel, matchingPaths))

        val replacementPoint = "$.a.b[*].c.d.e"
        assertEquals(listOf(ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']['e']", "")),
                resolveTargetPaths(jpathCtx, replacementPoint, matchingPaths))
    }

    @Test
    fun testRelativePathResolutionDownwardOnly() {
        val queryPath = "$['a']['b'][*]"
        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val insertionPoint = "@.c.d+e"

        assertEquals(listOf(
                ResolvedPaths.of("\$['a']['b'][0]", "\$['a']['b'][0]['c']['d']", "e"),
                ResolvedPaths.of("\$['a']['b'][1]", "\$['a']['b'][1]['c']['d']", "e")),
                resolveTargetPaths(jpathCtx, insertionPoint, matchingPaths))

        val replacementPoint = "@.c.d.e"
        assertEquals(listOf(
                ResolvedPaths.of("\$['a']['b'][0]", "\$['a']['b'][0]['c']['d']['e']", ""),
                ResolvedPaths.of("\$['a']['b'][1]", "\$['a']['b'][1]['c']['d']['e']", "")),
                resolveTargetPaths(jpathCtx, replacementPoint, matchingPaths))

        val queryPath2 = "$.a.b[*].c.d[?(@.e == 'foo')]"
        val matchingPaths2: List<String> = jpathCtx.read(queryPath2)

        val replacementPoint2 = "@.e"
        assertEquals(listOf(ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']['e']", "")),
                resolveTargetPaths(jpathCtx, replacementPoint2, matchingPaths2))

        val insertionPoint2 = "@+e2"
        assertEquals(listOf(ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", "e2")),
                resolveTargetPaths(jpathCtx, insertionPoint2, matchingPaths2))
    }

    @Test
    fun testRelativePathResolutionUpAndDown() {
        val queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]"
        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val insertionPoint = "@^c^b+somethingNew"
        assertEquals(listOf(ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]", "somethingNew")),
                resolveTargetPaths(jpathCtx, insertionPoint, matchingPaths))

        val insertionPointAtSameLevel = "@^c.d+e2"
        assertEquals(listOf(ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", "e2")),
                resolveTargetPaths(jpathCtx, insertionPointAtSameLevel, matchingPaths))

        val replacementPoint = "@^c.d.e"
        assertEquals(listOf(ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']['e']", "")),
                resolveTargetPaths(jpathCtx, replacementPoint, matchingPaths))
    }

    @Test
    @Ignore("This feature not yet supported, ^^up")
    fun testDeepBumpUp() {
        val queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]"
        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        // go up any number of steps to the first `b`
        val insertionPoint = "@^^b+somethingNew"
        assertEquals(listOf(ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]", "somethingNew")),
                resolveTargetPaths(jpathCtx, insertionPoint, matchingPaths))
    }

    @Test
    fun testRelativePath_from_property() {
        val queryPath = "$.a.b[*].c.d.e"
        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val goNowhere = "@" // stay in e
        assertEquals(listOf(
                ResolvedPaths.of("$['a']['b'][0]['c']['d']['e']", "$['a']['b'][0]['c']['d']['e']", ""),
                ResolvedPaths.of("$['a']['b'][1]['c']['d']['e']", "$['a']['b'][1]['c']['d']['e']", "")),
                resolveTargetPaths(jpathCtx, goNowhere, matchingPaths))

        val goUpOne = "@^d" // up to d
        assertEquals(listOf(
                ResolvedPaths.of("$['a']['b'][0]['c']['d']['e']", "$['a']['b'][0]['c']['d']", ""),
                ResolvedPaths.of("$['a']['b'][1]['c']['d']['e']", "$['a']['b'][1]['c']['d']", "")),
                resolveTargetPaths(jpathCtx, goUpOne, matchingPaths))

        val insertionPointAtSameLevel = "@^d+e2" // up to 'd', add new property
        val insertionPoints = resolveTargetPaths(jpathCtx, insertionPointAtSameLevel, matchingPaths)

        assertEquals(listOf(
                ResolvedPaths.of("$['a']['b'][0]['c']['d']['e']", "$['a']['b'][0]['c']['d']", "e2"),
                ResolvedPaths.of("$['a']['b'][1]['c']['d']['e']", "$['a']['b'][1]['c']['d']", "e2")),
                insertionPoints)

        // add new item so later reference to it works
        insertionPoints.forEachIndexed { idx, oneInsert ->
            applyUpdatePath(jpathCtx, oneInsert.targetBasePath, oneInsert.targetUpdatePath, "abc$idx")
        }

        val refPropertyAtSameLevel = "@^d.e2" // up to 'd', reference other property
        assertEquals(listOf(
                ResolvedPaths.of("$['a']['b'][0]['c']['d']['e']", "$['a']['b'][0]['c']['d']['e2']", ""),
                ResolvedPaths.of("$['a']['b'][1]['c']['d']['e']", "$['a']['b'][1]['c']['d']['e2']", "")),
                resolveTargetPaths(jpathCtx, refPropertyAtSameLevel, matchingPaths))

    }

    @Test
    fun testRelativePath_from_map() {
        val queryPath = "$.a.b[*].c.d"
        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val goNowhere = "@" // stay in 'd'
        assertEquals(listOf(
                ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", ""),
                ResolvedPaths.of("$['a']['b'][1]['c']['d']", "$['a']['b'][1]['c']['d']", "")),
                resolveTargetPaths(jpathCtx, goNowhere, matchingPaths))

        val goUpOne = "@^c" // up to 'c'
        assertEquals(listOf(
                ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']", ""),
                ResolvedPaths.of("$['a']['b'][1]['c']['d']", "$['a']['b'][1]['c']", "")),
                resolveTargetPaths(jpathCtx, goUpOne, matchingPaths))

        val insertionPointAtSameLevel = "@+e2" // stay in 'd', add new property
        val insertionPoints = resolveTargetPaths(jpathCtx, insertionPointAtSameLevel, matchingPaths)
        assertEquals(listOf(
                ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", "e2"),
                ResolvedPaths.of("$['a']['b'][1]['c']['d']", "$['a']['b'][1]['c']['d']", "e2")),
                insertionPoints)

        // add new item so later reference to it works
        insertionPoints.forEachIndexed { idx, oneInsert ->
            applyUpdatePath(jpathCtx, oneInsert.targetBasePath, oneInsert.targetUpdatePath, "abc$idx")
        }

        val refPropertyAtSameLevel = "@.e2" // stay in 'd', reference other property
        assertEquals(listOf(
                ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']['e2']", ""),
                ResolvedPaths.of("$['a']['b'][1]['c']['d']", "$['a']['b'][1]['c']['d']['e2']", "")),
                resolveTargetPaths(jpathCtx, refPropertyAtSameLevel, matchingPaths))

    }

    @Test
    fun testRelativePath_from_whole_array() {
        // normally bumping up and traversing an array goes to the element, but if we select the array itself as a
        // starting point, test what happens...

        val queryPath = "$.a.b"
        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val sameObject = "@" // stay in 'b'
        assertEquals(listOf(ResolvedPaths.of("$['a']['b']", "$['a']['b']", "")),
                resolveTargetPaths(jpathCtx, sameObject, matchingPaths))

        val goUpOne = "@^a" // go up to 'a'
        assertEquals(listOf(ResolvedPaths.of("$['a']['b']", "$['a']", "")),
                resolveTargetPaths(jpathCtx, goUpOne, matchingPaths))

        val refIndexAtSameLevel = "@[1]" // stay in 'b' but specific index
        assertEquals(listOf(ResolvedPaths.of("$['a']['b']", "$['a']['b'][1]", "")),
                resolveTargetPaths(jpathCtx, refIndexAtSameLevel, matchingPaths))

        val insertionPointAtSameLevel = "@+[+]" // stay in 'b' but add new item
        val insertionInfo = resolveTargetPaths(jpathCtx, insertionPointAtSameLevel, matchingPaths)
        assertEquals(listOf(ResolvedPaths.of("$['a']['b']", "$['a']['b']", "[+]")),
                insertionInfo)

        applyUpdatePath(jpathCtx, insertionInfo.first().targetBasePath, insertionInfo.first().targetUpdatePath, "whatever")
    }

    // notes about pathing through arrays
    //
    //  start at: a.b[0].c.d[1].e
    //
    //  @^d          goes to d[1]
    //  @^d+[+]      goes to d to add an array item
    //  @^d[0]       goes to array item d[0]
    //  @^d^c^b      goes to b[0]
    //  @^d^c^b+[*]  goes to b to update all items in the array
    //
    //  Therefore the rule is simple:  up paths go to the item in the array, down paths can go to the array itself


    @Test
    fun testRelativePath_from_indexed_item_in_array() {
        val queryPath = "$.a.b[0]"
        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val sameObject = "@" // stay in 'b[0]'
        assertEquals(listOf(ResolvedPaths.of("$['a']['b'][0]", "$['a']['b'][0]", "")),
                resolveTargetPaths(jpathCtx, sameObject, matchingPaths))

        val goUpOneObject = "@^a" // go up to 'a'
        assertEquals(listOf(ResolvedPaths.of("$['a']['b'][0]", "$['a']", "")),
                resolveTargetPaths(jpathCtx, goUpOneObject, matchingPaths))


        val insertionPointAtSameLevelShort = "@+[+]" // same array but change index
        val insertionInfo = resolveTargetPaths(jpathCtx, insertionPointAtSameLevelShort, matchingPaths)
        assertEquals(listOf(ResolvedPaths.of("$['a']['b'][0]", "$['a']['b']", "[+]")),
                insertionInfo)

        val insertInSameArray1 = "@^a+b[+]" // round about way to stay stay in 'b', add new item
        assertEquals(listOf(
                ResolvedPaths.of("$['a']['b'][0]", "$['a']", "b[+]")),
                resolveTargetPaths(jpathCtx, insertInSameArray1, matchingPaths))

        // failure cases, if in the array item, cannot go to the array itself otherwise this would be ambigious if you
        // wanted to go up to higher object, or wanted to be in array.
        try {
            val goUpOneToArray = "@^b" // go up to 'b'
            assertEquals(listOf(ResolvedPaths.of("$['a']['b'][0]", "$['a']['b']", "")),
                    resolveTargetPaths(jpathCtx, goUpOneToArray, matchingPaths))

            fail("Cannot path from array item to its own array")
        } catch (ex: IllegalStateException) {
            // yay!
        }

        try {
            val refIndexAtSameLevel = "@^b[1]" // stay in 'b' then down to specific index
            assertEquals(listOf(ResolvedPaths.of("$['a']['b'][0]", "$['a']['b'][1]", "")),
                    resolveTargetPaths(jpathCtx, refIndexAtSameLevel, matchingPaths))
            fail("Cannot path from array item to its own array")
        } catch (ex: IllegalStateException) {
            // yay!
        }

        try {
            val insertionPointAtSameLevel = "@^b+[+]" // up to 'b' and add new item
            assertEquals(listOf(ResolvedPaths.of("$['a']['b'][0]", "$['a']['b']", "[+]")),
                    resolveTargetPaths(jpathCtx, insertionPointAtSameLevel, matchingPaths))
            fail("Cannot path from array item to its own array")
        } catch (ex: IllegalStateException) {
            // yay!
        }

        try {
            val insertInSameArray2 = "@^b^a+b[+]" // more crazy round about way to stay stay in 'b', add new item
            assertEquals(listOf(
                    ResolvedPaths.of("$['a']['b'][0]", "$['a']", "b[+]")),
                    resolveTargetPaths(jpathCtx, insertInSameArray2, matchingPaths))
            fail("Cannot path from array item to its own array")
        } catch (ex: IllegalStateException) {
            // yay!
        }

    }

    @Test
    fun testCasesFailingFromConcatTesting() {
        val queryPath = "$.states[*].name"

        val relativePath = "@^states+cities[*].stateName" // go up from name to states[x] down to all cities, and add stateName
        val absolutePath = "$.states[*]+cities[*].stateName" // prefix match into states[x], then down to all cities, and add stateName

        val popTooFar = "@^states"

        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        assertEquals(listOf(
                ResolvedPaths.of("$['states'][0]['name']", "$['states'][0]", "cities[*].stateName"),
                ResolvedPaths.of("$['states'][1]['name']", "$['states'][1]", "cities[*].stateName")),
                resolveTargetPaths(jpathCtx, relativePath, matchingPaths))

        assertEquals(listOf(
                ResolvedPaths.of("$['states'][0]['name']", "$['states'][0]", "cities[*].stateName"),
                ResolvedPaths.of("$['states'][1]['name']", "$['states'][1]", "cities[*].stateName")),
                resolveTargetPaths(jpathCtx, absolutePath, matchingPaths))


        assertEquals(listOf(
                ResolvedPaths.of("$['states'][0]['name']", "$['states'][0]", ""),
                ResolvedPaths.of("$['states'][1]['name']", "$['states'][1]", "")),
                resolveTargetPaths(jpathCtx, popTooFar, matchingPaths))
    }


    @Test
    fun testUpdates() {
        val queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]"
        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val insertionPoint = "@^c^b+somethingNew"
        val insertionInfo = resolveTargetPaths(jpathCtx, insertionPoint, matchingPaths)
        insertionInfo.forEach { (_, basePath, updatePath) ->
            applyUpdatePath(jpathCtx, basePath, updatePath, "HOWDY!")
        }

        val checkInsert: String = JsonPath.compile("$.a.b[0].somethingNew").read<List<String>>(jpathCtx.json<Any>(), jvalueListConfig).first()
        assertEquals("HOWDY!", checkInsert)

        val insertionPointAtSameLevel = "@^c.d+e2"
        val insertionInfoAtSameLevel = resolveTargetPaths(jpathCtx, insertionPointAtSameLevel, matchingPaths)
        insertionInfoAtSameLevel.forEach { (_, basePath, updatePath) ->
            applyUpdatePath(jpathCtx, basePath, updatePath, "HOWDY!")
        }

        val checkInsert2: String = JsonPath.compile("$.a.b[0].c.d.e2").read<List<String>>(jpathCtx.json<Any>(), jvalueListConfig).first()
        assertEquals("HOWDY!", checkInsert2)


        val replacementPoint = "@^c.d.e"
        val replacementInfo = resolveTargetPaths(jpathCtx, replacementPoint, matchingPaths)
        replacementInfo.forEach { (_, basePath, updatePath) ->
            applyUpdatePath(jpathCtx, basePath, updatePath, "HOWDY!")
        }

        val checkInsert3: String = JsonPath.compile("$.a.b[0].c.d.e").read<List<String>>(jpathCtx.json<Any>(), jvalueListConfig).first()
        assertEquals("HOWDY!", checkInsert3)

        val insertionPointNewArray = "@^c^b+newArray[+].value1"
        val insertionInfoNewArray = resolveTargetPaths(jpathCtx, insertionPointNewArray, matchingPaths)
        insertionInfoNewArray.forEach { (_, basePath, updatePath) ->
            applyUpdatePath(jpathCtx, basePath, updatePath, "HOWDY 1")
        }

        val insertionPointNewArray2 = "@^c^b+newArray[*].value2"
        val insertionInfoNewArray2 = resolveTargetPaths(jpathCtx, insertionPointNewArray2, matchingPaths)
        insertionInfoNewArray2.forEach { (_, basePath, updatePath) ->
            applyUpdatePath(jpathCtx, basePath, updatePath, "HOWDY 2")
        }
        val checkInsertInNewArray1: String = JsonPath.compile("$.a.b[0].newArray[0].value1").read<List<String>>(jpathCtx.json<Any>(), jvalueListConfig).first()
        assertEquals("HOWDY 1", checkInsertInNewArray1)
        val checkInsertInNewArray2: String = JsonPath.compile("$.a.b[0].newArray[0].value2").read<List<String>>(jpathCtx.json<Any>(), jvalueListConfig).first()
        assertEquals("HOWDY 2", checkInsertInNewArray2)
    }

}