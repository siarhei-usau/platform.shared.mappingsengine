package com.ebsco.platform.shared.mappingsengine.core

import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class TestTransforms : BasePathTest() {
    private fun makeContext(): JsonTransformerContext {
        return JsonTransformerContext(jpathCtx.json(), jpathConfig, jvalueConfig, jvalueListConfig)
    }

    @Test
    fun testRenameTransform() {
        val queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]"
        val checkPath = "$.a.b[*].c.renamed[?(@.e == 'foo')]"
        val safePath = "$.a.b[*].c.d[?(@.e == 'miss')]"

        // test move `a.b.c.d[e == 'foo']` to `a.b.c.renamed`

        val context = makeContext()

        // value is there before in the source, but not target position
        assertEquals(listOf("$['a']['b'][0]['c']['d']"), context.queryForPaths(queryPath))
        assertEquals(emptyList(), context.queryForPaths(checkPath))

        // and this one is the safety path that should not be touched
        assertEquals(listOf("$['a']['b'][1]['c']['d']"), context.queryForPaths(safePath))

        RenameJsonTransform(queryPath, "@^c+renamed").apply(context)
        printJson(context.jsonObject)

        // value is in new path but not old
        assertEquals(emptyList(), context.queryForPaths(queryPath))
        assertEquals(listOf("$['a']['b'][0]['c']['renamed']"), context.queryForPaths(checkPath))

        // and this one is the safety path that should not be touched
        assertEquals(listOf("$['a']['b'][1]['c']['d']"), context.queryForPaths(safePath))
    }

    @Test
    fun testCopyTransform_SingleValue() {
        val queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]"
        val checkPath = "$.a.b[*].c.renamed[?(@.e == 'foo')]"
        val safePath = "$.a.b[*].c.d[?(@.e == 'miss')]"

        // test move `a.b.c.d[e == 'foo']` to `a.b.c.renamed`

        val context = makeContext()

        // value is there before in the source, but not target position
        assertEquals(listOf("$['a']['b'][0]['c']['d']"), context.queryForPaths(queryPath))
        assertEquals(emptyList(), context.queryForPaths(checkPath))

        // and this one is the safety path that should not be touched
        assertEquals(listOf("$['a']['b'][1]['c']['d']"), context.queryForPaths(safePath))

        CopyJsonTransform(queryPath, "@^c+renamed").apply(context)
        printJson(context.jsonObject)

        // value is in new path AND in the old
        assertEquals(listOf("$['a']['b'][0]['c']['d']"), context.queryForPaths(queryPath))
        assertEquals(listOf("$['a']['b'][0]['c']['renamed']"), context.queryForPaths(checkPath))

        // and this one is the safety path that should not be touched
        assertEquals(listOf("$['a']['b'][1]['c']['d']"), context.queryForPaths(safePath))
    }

    @Test
    fun testCopyTransform_TargetIntoArray() {
        val queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]"
        val checkPath = "$.a.b[*].c.copied[?(@.e == 'foo')]"
        val checkPath2 = "$.a.b[*].c.copied[?(@.e == 'miss')]"
        val safePath = "$.a.b[*].c.d[?(@.e == 'miss')]"

        // test move `a.b.c.d[e == 'foo']` to `a.b.c.renamed`

        val context = makeContext()

        // value is there before in the source, but not target position
        assertEquals(listOf("$['a']['b'][0]['c']['d']"), context.queryForPaths(queryPath))
        assertEquals(emptyList(), context.queryForPaths(checkPath))

        // and this one is the safety path that should not be touched
        assertEquals(listOf("$['a']['b'][1]['c']['d']"), context.queryForPaths(safePath))

        CopyJsonTransform(queryPath, "@^c+copied[+]").apply(context)
        printJson(context.jsonObject)

        // value is in new path AND in the old
        assertEquals(listOf("$['a']['b'][0]['c']['d']"), context.queryForPaths(queryPath))
        assertEquals(listOf("$['a']['b'][0]['c']['copied'][0]"), context.queryForPaths(checkPath))

        // and this one is the safety path that should not be touched
        assertEquals(listOf("$['a']['b'][1]['c']['d']"), context.queryForPaths(safePath))

        // now copy the safety value over to a whole other part of the tree to go with the other copied value
        CopyJsonTransform(safePath, "@^c^b^a.b[0].c+copied[+]").apply(context)
        printJson(context.jsonObject)

        assertEquals(listOf("$['a']['b'][0]['c']['d']"), context.queryForPaths(queryPath))
        assertEquals(listOf("$['a']['b'][0]['c']['copied'][0]"), context.queryForPaths(checkPath))

        // our safety value has made it into the array as well!
        assertEquals(listOf("$['a']['b'][0]['c']['copied'][1]"), context.queryForPaths(checkPath2))

    }

    @Test
    fun testDeleteTransform() {
        val queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]"
        val context = makeContext()
        assertEquals(listOf("$['a']['b'][0]['c']['d']"), context.queryForPaths(queryPath))

        DeleteJsonTransform(queryPath).apply(context)
        printJson(context.jsonObject)

        // values are gone!
        assertEquals(emptyList(), context.queryForPaths(queryPath))
    }

    @Test
    fun testConcatTransform_InSameObjects() {

        val context = makeContext()

        // contact firstname and lastname within same objects

        val fromPaths = listOf("$.people[*].lastname", "$.people[*].firstname")
        ConcatJsonTransform(fromPaths, ", ", "$.people[*]+fullname").apply(context)
        printJson(context.jsonObject)

        assertEquals("Smith, David", context.queryForValue("$.people[0].fullname"))
        assertEquals("Stark, Michael", context.queryForValue("$.people[1].fullname"))
    }

    @Test
    fun testConcatTransform_MixedObjectConcat_AbsoluteTarget() {

        val context = makeContext()

        /*
            from { "states": [{ "name": "Colorado", "cities": [{ "name": "Denver" }, { "name": "Boulder" }] , ...] }
            to   { "states": [{ "name": "Colorado", "cities": [{ "name": "Denver", "cityState": "Denver, Colorado" },
                                                               { "name": "Boulder", "cityState": "Boulder, Colorado" }] , ...] }
         */

        CopyJsonTransform("$.states[*].name", "@^states+cities[*].stateName").apply(context)
        printJson(context.jsonObject, "After Copy")

        val fromPaths = listOf("$.states[*].cities[*].name", "$.states[*].cities[*].stateName")

        // absolute target path is the safest approach...
        ConcatJsonTransform(fromPaths, ", ", "$.states[*].cities[*]+cityState").apply(context)
        printJson(context.jsonObject, "After Concat")

        DeleteJsonTransform("$.states[*].cities[*].stateName")
        printJson(context.jsonObject, "After Delete")

        assertEquals("Denver, Colorado", context.queryForValue("$.states[0].cities[0].cityState"))
        assertEquals("Boulder, Colorado", context.queryForValue("$.states[0].cities[1].cityState"))
        assertEquals("San Francisco, California", context.queryForValue("$.states[1].cities[0].cityState"))
        assertEquals("Santa Cruz, California", context.queryForValue("$.states[1].cities[1].cityState"))
    }

    @Test
    fun testConcatTransform_MixedObjectConcat_RelativeTarget() {

        val context = makeContext()

        /*
            from { "states": [{ "name": "Colorado", "cities": [{ "name": "Denver" }, { "name": "Boulder" }] , ...] }
            to   { "states": [{ "name": "Colorado", "cities": [{ "name": "Denver", "cityState": "Denver, Colorado" },
                                                               { "name": "Boulder", "cityState": "Boulder, Colorado" }] , ...] }
         */

        CopyJsonTransform("$.states[*].name", "@^states+cities[*].stateName").apply(context)
        printJson(context.jsonObject, "After Copy")

        val fromPaths = listOf("$.states[*].cities[*].name", "$.states[*].cities[*].stateName")

        // relative target path is trustworthy here because all the from nodes are at the same level
        ConcatJsonTransform(fromPaths, ", ", "@^cities+cityState").apply(context)
        printJson(context.jsonObject, "After Concat")

        DeleteJsonTransform("$.states[*].cities[*].stateName")
        printJson(context.jsonObject, "After Delete")

        assertEquals("Denver, Colorado", context.queryForValue("$.states[0].cities[0].cityState"))
        assertEquals("Boulder, Colorado", context.queryForValue("$.states[0].cities[1].cityState"))
        assertEquals("San Francisco, California", context.queryForValue("$.states[1].cities[0].cityState"))
        assertEquals("Santa Cruz, California", context.queryForValue("$.states[1].cities[1].cityState"))
    }

    @Test
    fun testConcatTransform_MixedObjectConcat() {

        val context = makeContext()

        /*
            from { "states": [{ "name": "Colorado", "cities": [{ "name": "Denver" }, { "name": "Boulder" }] , ...] }
            to   { "states": [{ "name": "Colorado", "cities": [{ "name": "Denver", "cityState": "Denver, Colorado" },
                                                               { "name": "Boulder", "cityState": "Boulder, Colorado" }] , ...] }
         */

        val fromPaths = listOf("$.states[*].cities[*].name", "$.states[*].name")

        // need absolute target path here because the from nodes are at different levels
        ConcatJsonTransform(fromPaths, ", ", "$.states[*].cities[*]+cityState").apply(context)
        printJson(context.jsonObject, "After Concat")

        assertEquals("Denver, Colorado", context.queryForValue("$.states[0].cities[0].cityState"))
        assertEquals("Boulder, Colorado", context.queryForValue("$.states[0].cities[1].cityState"))
        assertEquals("San Francisco, California", context.queryForValue("$.states[1].cities[0].cityState"))
        assertEquals("Santa Cruz, California", context.queryForValue("$.states[1].cities[1].cityState"))
    }

    @Test
    fun testConcatTransform_MixedObjectConcat_BadRelativeTarget() {

        val context = makeContext()

        val fromPaths = listOf("$.states[*].cities[*].name", "$.states[*].name")

        try {
            // this relative path is bad, because it will mean different things
            ConcatJsonTransform(fromPaths, ", ", "@^cities+cityState").apply(context)
            fail("This should cause a failed path, since the path changes depending on the source node, and is invalid at times")
        } catch (ex: IllegalStateException) {
            // success!
        }
    }

    @Test
    fun testConcatTransform_MultipleOfSameIntoSingle_absoluteTarget() {

        val context = makeContext()

        /*
            from { "states": [{ "name": "Colorado", "cities": [{ "name": "Denver" }, { "name": "Boulder" }] , ...] }
            to   { "states": [{ "name": "Colorado", "cityList": "Denver, Boulder", "cities": [{ "name": "Denver", ... }, { "name": "Boulder", ... }, ...] }
         */

        val fromPaths = listOf("$.states[*].cities[*].name")

        // need absolute target path here because the from nodes are at different levels
        ConcatJsonTransform(fromPaths, ", ", "$.states[*]+cityList").apply(context)
        printJson(context.jsonObject, "After Concat")

        assertEquals("Denver, Boulder", context.queryForValue("$.states[0].cityList"))
        assertEquals("San Francisco, Santa Cruz", context.queryForValue("$.states[1].cityList"))
    }


    @Test
    fun testConcatTransform_MultipleOfSameIntoSingle_relativeTarget() {

        val context = makeContext()

        /*
            from { "states": [{ "name": "Colorado", "cities": [{ "name": "Denver" }, { "name": "Boulder" }] , ...] }
            to   { "states": [{ "name": "Colorado", "cityList": "Denver, Boulder", "cities": [{ "name": "Denver", ... }, { "name": "Boulder", ... }, ...] }
         */

        val fromPaths = listOf("$.states[*].cities[*].name")

        // need absolute target path here because the from nodes are at different levels
        ConcatJsonTransform(fromPaths, ", ", "@^cities^states+cityList").apply(context)
        printJson(context.jsonObject, "After Concat")

        assertEquals("Denver, Boulder", context.queryForValue("$.states[0].cityList"))
        assertEquals("San Francisco, Santa Cruz", context.queryForValue("$.states[1].cityList"))
    }

    @Test
    fun testCopyTransform_MultipleOfSameIntoSingle_absoluteTarget() {

        val context = makeContext()

        /*
            from { "states": [{ "name": "Colorado", "cities": [{ "name": "Denver" }, { "name": "Boulder" }] , ...] }
            to   { "states": [{ "name": "Colorado", "cityList": ["Denver", "Boulder"], "cities": [{ "name": "Denver", ... }, { "name": "Boulder", ... }, ...] }
         */

        // need absolute target path here because the from nodes are at different levels
        CopyJsonTransform("$.states[*].cities[*].name", "$.states[*]+cityList[+]").apply(context)
        printJson(context.jsonObject, "After Concat")

        assertEquals(listOf("Denver", "Boulder"), context.queryForValue("$.states[0].cityList"))
        assertEquals(listOf("San Francisco", "Santa Cruz"), context.queryForValue("$.states[1].cityList"))
    }

    @Test
    fun testCopyTransform_MultipleOfSameIntoSingle_relativeTarget() {

        val context = makeContext()

        /*
            from { "states": [{ "name": "Colorado", "cities": [{ "name": "Denver" }, { "name": "Boulder" }] , ...] }
            to   { "states": [{ "name": "Colorado", "cityList": ["Denver", "Boulder"], "cities": [{ "name": "Denver", ... }, { "name": "Boulder", ... }, ...] }
         */

        // need absolute target path here because the from nodes are at different levels
        CopyJsonTransform("$.states[*].cities[*].name", "@^cities^states+cityList[+]").apply(context)
        printJson(context.jsonObject, "After Concat")

        assertEquals(listOf("Denver", "Boulder"), context.queryForValue("$.states[0].cityList"))
        assertEquals(listOf("San Francisco", "Santa Cruz"), context.queryForValue("$.states[1].cityList"))
    }
}