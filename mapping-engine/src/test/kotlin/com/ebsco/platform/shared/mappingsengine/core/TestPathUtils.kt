package com.ebsco.platform.shared.mappingsengine.core

import com.jayway.jsonpath.JsonPath
import org.junit.Test
import kotlin.test.assertEquals

class TestPathUtils : BasePathTest() {

    @Test
    fun testAbsolutePathResolution() {
        val queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]"
        // common prefix will start at the point where the prefix of the searched path is in common

        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val insertionPoint = "$.a.b[*]+somethingNew"
        assertEquals(listOf(ResolvedPaths("$['a']['b'][0]['c']['d']", "$['a']['b'][0]", "somethingNew")),
                jpathCtx.resolveTargetPaths(insertionPoint, matchingPaths))

        val insertionPointAtSameLevel = "$.a.b[*].c.d+e2"
        assertEquals(listOf(ResolvedPaths("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", "e2")),
                jpathCtx.resolveTargetPaths(insertionPointAtSameLevel, matchingPaths))

        val replacementPoint = "$.a.b[*].c.d.e"
        assertEquals(listOf(ResolvedPaths("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']['e']", "")),
                jpathCtx.resolveTargetPaths(replacementPoint, matchingPaths))
    }

    @Test
    fun testRelativePathResolutionDownwardOnly() {
        val queryPath = "$['a']['b'][*]"
        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val insertionPoint = "@.c.d+e"

        assertEquals(listOf(
                ResolvedPaths("\$['a']['b'][0]", "\$['a']['b'][0]['c']['d']", "e"),
                ResolvedPaths("\$['a']['b'][1]", "\$['a']['b'][1]['c']['d']", "e")),
                jpathCtx.resolveTargetPaths(insertionPoint, matchingPaths))

        val replacementPoint = "@.c.d.e"
        assertEquals(listOf(
                ResolvedPaths("\$['a']['b'][0]", "\$['a']['b'][0]['c']['d']['e']", ""),
                ResolvedPaths("\$['a']['b'][1]", "\$['a']['b'][1]['c']['d']['e']", "")),
                jpathCtx.resolveTargetPaths(replacementPoint, matchingPaths))

        val queryPath2 = "$.a.b[*].c.d[?(@.e == 'foo')]"
        val matchingPaths2: List<String> = jpathCtx.read(queryPath2)

        val replacementPoint2 = "@.e"
        assertEquals(listOf(ResolvedPaths("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']['e']", "")),
                jpathCtx.resolveTargetPaths(replacementPoint2, matchingPaths2))

        val insertionPoint2 = "@+e2"
        assertEquals(listOf(ResolvedPaths("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", "e2")),
                jpathCtx.resolveTargetPaths(insertionPoint2, matchingPaths2))
    }

    @Test
    fun testRelativePathResolutionUpAndDown() {
        val queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]"
        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val insertionPoint = "@^c^b+somethingNew"
        assertEquals(listOf(ResolvedPaths("$['a']['b'][0]['c']['d']", "$['a']['b'][0]", "somethingNew")),
                jpathCtx.resolveTargetPaths(insertionPoint, matchingPaths))

        val insertionPointAtSameLevel = "@^c.d+e2"
        assertEquals(listOf(ResolvedPaths("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", "e2")),
                jpathCtx.resolveTargetPaths(insertionPointAtSameLevel, matchingPaths))

        val replacementPoint = "@^c.d.e"
        assertEquals(listOf(ResolvedPaths("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']['e']", "")),
                jpathCtx.resolveTargetPaths(replacementPoint, matchingPaths))

        // TODO not supporting deep bump up
        //  val target2 = "@^^b+somethingNew"   // traverse unknown steps up the tree until b is found within the found path

    }

    @Test
    fun testRelativePath_from_property() {
        val queryPath = "$.a.b[*].c.d.e"
        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val goNowhere = "@" // stay in e
        assertEquals(listOf(
                ResolvedPaths("$['a']['b'][0]['c']['d']['e']", "$['a']['b'][0]['c']['d']['e']", ""),
                ResolvedPaths("$['a']['b'][1]['c']['d']['e']", "$['a']['b'][1]['c']['d']['e']", "")),
                jpathCtx.resolveTargetPaths(goNowhere, matchingPaths))

        val goUpOne = "@^d" // up to d
        assertEquals(listOf(
                ResolvedPaths("$['a']['b'][0]['c']['d']['e']", "$['a']['b'][0]['c']['d']", ""),
                ResolvedPaths("$['a']['b'][1]['c']['d']['e']", "$['a']['b'][1]['c']['d']", "")),
                jpathCtx.resolveTargetPaths(goUpOne, matchingPaths))

        val insertionPointAtSameLevel = "@^d+e2" // up to 'd', add new property
        val insertionPoints = jpathCtx.resolveTargetPaths(insertionPointAtSameLevel, matchingPaths)

        assertEquals(listOf(
                ResolvedPaths("$['a']['b'][0]['c']['d']['e']", "$['a']['b'][0]['c']['d']", "e2"),
                ResolvedPaths("$['a']['b'][1]['c']['d']['e']", "$['a']['b'][1]['c']['d']", "e2")),
                insertionPoints)

        // add new item so later reference to it works
        insertionPoints.forEachIndexed { idx, oneInsert ->
            jpathCtx.applyUpdatePath(oneInsert.targetBasePath, oneInsert.targetUpdatePath, "abc$idx")
        }

        val refPropertyAtSameLevel = "@^d.e2" // up to 'd', reference other property
        assertEquals(listOf(
                ResolvedPaths("$['a']['b'][0]['c']['d']['e']", "$['a']['b'][0]['c']['d']['e2']", ""),
                ResolvedPaths("$['a']['b'][1]['c']['d']['e']", "$['a']['b'][1]['c']['d']['e2']", "")),
                jpathCtx.resolveTargetPaths(refPropertyAtSameLevel, matchingPaths))

    }

    @Test
    fun testRelativePath_from_map() {
        val queryPath = "$.a.b[*].c.d"
        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val goNowhere = "@" // stay in 'd'
        assertEquals(listOf(
                ResolvedPaths("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", ""),
                ResolvedPaths("$['a']['b'][1]['c']['d']", "$['a']['b'][1]['c']['d']", "")),
                jpathCtx.resolveTargetPaths(goNowhere, matchingPaths))

        val goUpOne = "@^c" // up to 'c'
        assertEquals(listOf(
                ResolvedPaths("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']", ""),
                ResolvedPaths("$['a']['b'][1]['c']['d']", "$['a']['b'][1]['c']", "")),
                jpathCtx.resolveTargetPaths(goUpOne, matchingPaths))

        val insertionPointAtSameLevel = "@+e2" // stay in 'd', add new property
        val insertionPoints = jpathCtx.resolveTargetPaths(insertionPointAtSameLevel, matchingPaths)
        assertEquals(listOf(
                ResolvedPaths("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", "e2"),
                ResolvedPaths("$['a']['b'][1]['c']['d']", "$['a']['b'][1]['c']['d']", "e2")),
                insertionPoints)

        // add new item so later reference to it works
        insertionPoints.forEachIndexed { idx, oneInsert ->
            jpathCtx.applyUpdatePath(oneInsert.targetBasePath, oneInsert.targetUpdatePath, "abc$idx")
        }

        val refPropertyAtSameLevel = "@.e2" // stay in 'd', reference other property
        assertEquals(listOf(
                ResolvedPaths("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']['e2']", ""),
                ResolvedPaths("$['a']['b'][1]['c']['d']", "$['a']['b'][1]['c']['d']['e2']", "")),
                jpathCtx.resolveTargetPaths(refPropertyAtSameLevel, matchingPaths))

    }

    @Test
    fun testRelativePath_from_whole_array() {
        val queryPath = "$.a.b"
        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val sameObject = "@" // stay in 'b'
        assertEquals(listOf(ResolvedPaths("$['a']['b']", "$['a']['b']", "")),
                jpathCtx.resolveTargetPaths(sameObject, matchingPaths))

        val goUpOne = "@^a" // go up to 'a'
        assertEquals(listOf(ResolvedPaths("$['a']['b']", "$['a']", "")),
                jpathCtx.resolveTargetPaths(goUpOne, matchingPaths))

        val refIndexAtSameLevel = "@[1]" // stay in 'b' but specific index
        assertEquals(listOf(ResolvedPaths("$['a']['b']", "$['a']['b'][1]", "")),
                jpathCtx.resolveTargetPaths(refIndexAtSameLevel, matchingPaths))

        val insertionPointAtSameLevel = "@+[+]" // stay in 'b' but add new item
        val insertionInfo = jpathCtx.resolveTargetPaths(insertionPointAtSameLevel, matchingPaths)
        assertEquals(listOf(ResolvedPaths("$['a']['b']", "$['a']['b']", "[+]")),
                insertionInfo)

        jpathCtx.applyUpdatePath(insertionInfo.first().targetBasePath, insertionInfo.first().targetUpdatePath, "whatever")
    }


    @Test
    fun testRelativePath_from_indexed_item_in_array() {
        val queryPath = "$.a.b[0]"
        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        val sameObject = "@" // stay in 'b[0]'
        assertEquals(listOf(ResolvedPaths("$['a']['b'][0]", "$['a']['b'][0]", "")),
                jpathCtx.resolveTargetPaths(sameObject, matchingPaths))

        val goUpOne = "@^b" // go up to 'b'
        assertEquals(listOf(ResolvedPaths("$['a']['b'][0]", "$['a']['b']", "")),
                jpathCtx.resolveTargetPaths(goUpOne, matchingPaths))

        val refIndexAtSameLevel = "@^b[1]" // stay in 'b' then down to specific index
        assertEquals(listOf(ResolvedPaths("$['a']['b'][0]", "$['a']['b'][1]", "")),
                jpathCtx.resolveTargetPaths(refIndexAtSameLevel, matchingPaths))


        val insertionPointAtSameLevel = "@^b+[+]" // up to 'b' and add new item
        assertEquals(listOf(ResolvedPaths("$['a']['b'][0]", "$['a']['b']", "[+]")),
                jpathCtx.resolveTargetPaths(insertionPointAtSameLevel, matchingPaths))

        val insertInSameArray = "@^b^a+b[+]" // round about way to stay stay in 'b', add new item
        assertEquals(listOf(
                ResolvedPaths("$['a']['b'][0]", "$['a']", "b[+]")),
                jpathCtx.resolveTargetPaths(insertInSameArray, matchingPaths))


        // TODO: it isn't clear pathing through array items:
        //    start at: a.b[0].c.d[1].e
        //    @^d        goes to d[1]
        //    @^d^d      goes to d the array itself
        //    @^d^c      goes to c, but didn't have to go through the array item
        //    @^d^c^b    goes to b[0]
        //  etc.
        //
        //  this is a little confusing.  We need the ability to do either, or is it really only the item in the array that
        //  we care about and not the array itself?  We can say it is the item unless you add [] for some insert directive
        //  then it is the array.
        //
        //  So this would change the above to:
        //
        //  @^d          goes to d[1]
        //  @^d+[+]      goes to d to add an array item
        //  @^d[0]       goes to array item d[0]
        //  @^d^c^b      goes to b[0]
        //  @^d^c^b+[*]  goes to b to update all items in the array
        //
        //  Therefore the rule is simple:  up paths go to the item in the array, down paths can go to the array itself
        //
        // TODO: change all test cases to match this, and remove these comments.
    }

    @Test
    fun testCasesFailingFromConcatTesting() {
        val queryPath = "$.states[*].name"

        val relativePath = "@^states[*]+cities[*].stateName" // go up from name to states[x] down to all cities, and add stateName
        val absolutePath = "$.states[*]+cities[*].stateName" // prefix match into states[x], then down to all cities, and add stateName

        val popTooFar = "@^states"

        val matchingPaths: List<String> = jpathCtx.read(queryPath)

        assertEquals(listOf(
                ResolvedPaths("$['states'][0]['name']", "$['states'][0]", "cities[*].stateName"),
                ResolvedPaths("$['states'][1]['name']", "$['states'][1]", "cities[*].stateName")),
                jpathCtx.resolveTargetPaths(relativePath, matchingPaths))

        assertEquals(listOf(
                ResolvedPaths("$['states'][0]['name']", "$['states'][0]", "cities[*].stateName"),
                ResolvedPaths("$['states'][1]['name']", "$['states'][1]", "cities[*].stateName")),
                jpathCtx.resolveTargetPaths(absolutePath, matchingPaths))


        assertEquals(listOf(
                ResolvedPaths("$['states'][0]['name']", "$['states']", ""),
                ResolvedPaths("$['states'][1]['name']", "$['states']", "")),
                jpathCtx.resolveTargetPaths(popTooFar, matchingPaths))
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

        val checkInsert: String = JsonPath.compile("$.a.b[0].somethingNew").read<List<String>>(jpathCtx.json<Any>(), jvalueListConfig).first()
        assertEquals("HOWDY!", checkInsert)

        val insertionPointAtSameLevel = "@^c.d+e2"
        val insertionInfoAtSameLevel = jpathCtx.resolveTargetPaths(insertionPointAtSameLevel, matchingPaths)
        insertionInfoAtSameLevel.forEach { (_, basePath, updatePath) ->
            jpathCtx.applyUpdatePath(basePath, updatePath, "HOWDY!")
        }

        val checkInsert2: String = JsonPath.compile("$.a.b[0].c.d.e2").read<List<String>>(jpathCtx.json<Any>(), jvalueListConfig).first()
        assertEquals("HOWDY!", checkInsert2)


        val replacementPoint = "@^c.d.e"
        val replacementInfo = jpathCtx.resolveTargetPaths(replacementPoint, matchingPaths)
        replacementInfo.forEach { (_, basePath, updatePath) ->
            jpathCtx.applyUpdatePath(basePath, updatePath, "HOWDY!")
        }

        val checkInsert3: String = JsonPath.compile("$.a.b[0].c.d.e").read<List<String>>(jpathCtx.json<Any>(), jvalueListConfig).first()
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
        val checkInsertInNewArray1: String = JsonPath.compile("$.a.b[0].newArray[0].value1").read<List<String>>(jpathCtx.json<Any>(), jvalueListConfig).first()
        assertEquals("HOWDY 1", checkInsertInNewArray1)
        val checkInsertInNewArray2: String = JsonPath.compile("$.a.b[0].newArray[0].value2").read<List<String>>(jpathCtx.json<Any>(), jvalueListConfig).first()
        assertEquals("HOWDY 2", checkInsertInNewArray2)
    }

}