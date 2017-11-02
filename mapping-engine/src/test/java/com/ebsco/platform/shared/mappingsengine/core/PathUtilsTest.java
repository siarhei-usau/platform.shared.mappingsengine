package com.ebsco.platform.shared.mappingsengine.core;

import com.jayway.jsonpath.JsonPath;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.ebsco.platform.shared.mappingsengine.core.PathUtils.applyUpdatePath;
import static com.ebsco.platform.shared.mappingsengine.core.PathUtils.resolveTargetPaths;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PathUtilsTest extends BasePathTest {

    @Test
    public void testAbsolutePathResolution() {
        String queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]";
        // common prefix will start at the point where the prefix of the searched path is in common

        List<String> matchingPaths = jpathCtx.read(queryPath);

        String insertionPoint = "$.a.b[*]+somethingNew";
        resolveTargetPaths(jpathCtx, insertionPoint, matchingPaths, false);
        assertEquals(singletonList(ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]", "somethingNew")),
                resolveTargetPaths(jpathCtx, insertionPoint, matchingPaths, false));

        String insertionPointAtSameLevel = "$.a.b[*].c.d+e2";
        assertEquals(singletonList(ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", "e2")),
                resolveTargetPaths(jpathCtx, insertionPointAtSameLevel, matchingPaths, false));

        String replacementPoint = "$.a.b[*].c.d.e";
        assertEquals(singletonList(ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']['e']", "")),
                resolveTargetPaths(jpathCtx, replacementPoint, matchingPaths, false));
    }

    @Test
    public void testRelativePathResolutionDownwardOnly() {
        String queryPath = "$['a']['b'][*]";
        List<String> matchingPaths = jpathCtx.read(queryPath);

        String insertionPoint = "@.c.d+e";

        assertEquals(Arrays.asList(
                ResolvedPaths.of("$['a']['b'][0]", "$['a']['b'][0]['c']['d']", "e"),
                ResolvedPaths.of("$['a']['b'][1]", "$['a']['b'][1]['c']['d']", "e")),
                resolveTargetPaths(jpathCtx, insertionPoint, matchingPaths, false));

        String replacementPoint = "@.c.d.e";
        assertEquals(Arrays.asList(
                ResolvedPaths.of("$['a']['b'][0]", "$['a']['b'][0]['c']['d']['e']", ""),
                ResolvedPaths.of("$['a']['b'][1]", "$['a']['b'][1]['c']['d']['e']", "")),
                resolveTargetPaths(jpathCtx, replacementPoint, matchingPaths, false));

        String queryPath2 = "$.a.b[*].c.d[?(@.e == 'foo')]";
        List<String> matchingPaths2 = jpathCtx.read(queryPath2);

        String replacementPoint2 = "@.e";
        assertEquals(singletonList(ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']['e']", "")),
                resolveTargetPaths(jpathCtx, replacementPoint2, matchingPaths2, false));

        String insertionPoint2 = "@+e2";
        assertEquals(singletonList(ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", "e2")),
                resolveTargetPaths(jpathCtx, insertionPoint2, matchingPaths2));
    }

    @Test
    public void testRelativePathResolutionUpAndDown() {
        String queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]";
        List<String> matchingPaths = jpathCtx.read(queryPath);

        String insertionPoint = "@^c^b+somethingNew";
        assertEquals(singletonList(ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]", "somethingNew")),
                resolveTargetPaths(jpathCtx, insertionPoint, matchingPaths));

        String insertionPointAtSameLevel = "@^c.d+e2";
        assertEquals(singletonList(ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", "e2")),
                resolveTargetPaths(jpathCtx, insertionPointAtSameLevel, matchingPaths));

        String replacementPoint = "@^c.d.e";
        assertEquals(singletonList(ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']['e']", "")),
                resolveTargetPaths(jpathCtx, replacementPoint, matchingPaths));
    }

    @Test
    public void testRelativePath_from_property() {
        String queryPath = "$.a.b[*].c.d.e";
        List<String> matchingPaths = jpathCtx.read(queryPath);

        String goNowhere = "@"; // stay in e
        assertEquals(Arrays.asList(
                ResolvedPaths.of("$['a']['b'][0]['c']['d']['e']", "$['a']['b'][0]['c']['d']['e']", ""),
                ResolvedPaths.of("$['a']['b'][1]['c']['d']['e']", "$['a']['b'][1]['c']['d']['e']", "")),
                resolveTargetPaths(jpathCtx, goNowhere, matchingPaths));

        String goUpOne = "@^d"; // up to d
        assertEquals(Arrays.asList(
                ResolvedPaths.of("$['a']['b'][0]['c']['d']['e']", "$['a']['b'][0]['c']['d']", ""),
                ResolvedPaths.of("$['a']['b'][1]['c']['d']['e']", "$['a']['b'][1]['c']['d']", "")),
                resolveTargetPaths(jpathCtx, goUpOne, matchingPaths));

        String insertionPointAtSameLevel = "@^d+e2"; // up to 'd', add new property
        List<ResolvedPaths> insertionPoints = resolveTargetPaths(jpathCtx, insertionPointAtSameLevel, matchingPaths);

        assertEquals(Arrays.asList(
                ResolvedPaths.of("$['a']['b'][0]['c']['d']['e']", "$['a']['b'][0]['c']['d']", "e2"),
                ResolvedPaths.of("$['a']['b'][1]['c']['d']['e']", "$['a']['b'][1]['c']['d']", "e2")),
                insertionPoints);

        // add new item so later reference to it works
        for (int idx = 0; idx < insertionPoints.size(); idx++) {
            ResolvedPaths oneInsert = insertionPoints.get(idx);
            applyUpdatePath(jpathCtx, oneInsert.getTargetBasePath(), oneInsert.getTargetUpdatePath(), "abc" + idx);
        }

        String refPropertyAtSameLevel = "@^d.e2"; // up to 'd', reference other property
        assertEquals(Arrays.asList(
                ResolvedPaths.of("$['a']['b'][0]['c']['d']['e']", "$['a']['b'][0]['c']['d']['e2']", ""),
                ResolvedPaths.of("$['a']['b'][1]['c']['d']['e']", "$['a']['b'][1]['c']['d']['e2']", "")),
                resolveTargetPaths(jpathCtx, refPropertyAtSameLevel, matchingPaths));
    }

    @Test
    public void testRelativePath_from_map() {
        String queryPath = "$.a.b[*].c.d";
        List<String> matchingPaths = jpathCtx.read(queryPath);

        String goNowhere = "@"; // stay in 'd'
        assertEquals(Arrays.asList(
                ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", ""),
                ResolvedPaths.of("$['a']['b'][1]['c']['d']", "$['a']['b'][1]['c']['d']", "")),
                resolveTargetPaths(jpathCtx, goNowhere, matchingPaths));

        String goUpOne = "@^c"; // up to 'c'
        assertEquals(Arrays.asList(
                ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']", ""),
                ResolvedPaths.of("$['a']['b'][1]['c']['d']", "$['a']['b'][1]['c']", "")),
                resolveTargetPaths(jpathCtx, goUpOne, matchingPaths));

        String insertionPointAtSameLevel = "@+e2"; // stay in 'd', add new property
        List<ResolvedPaths> insertionPoints = resolveTargetPaths(jpathCtx, insertionPointAtSameLevel, matchingPaths);
        assertEquals(Arrays.asList(
                ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']", "e2"),
                ResolvedPaths.of("$['a']['b'][1]['c']['d']", "$['a']['b'][1]['c']['d']", "e2")),
                insertionPoints);

        // add new item so later reference to it works
        for (int idx = 0; idx < insertionPoints.size(); idx++) {
            ResolvedPaths oneInsert = insertionPoints.get(idx);
            applyUpdatePath(jpathCtx, oneInsert.getTargetBasePath(), oneInsert.getTargetUpdatePath(), "abc" + idx);
        }

        String refPropertyAtSameLevel = "@.e2"; // stay in 'd', reference other property
        assertEquals(Arrays.asList(
                ResolvedPaths.of("$['a']['b'][0]['c']['d']", "$['a']['b'][0]['c']['d']['e2']", ""),
                ResolvedPaths.of("$['a']['b'][1]['c']['d']", "$['a']['b'][1]['c']['d']['e2']", "")),
                resolveTargetPaths(jpathCtx, refPropertyAtSameLevel, matchingPaths));

    }

    @Test
    public void testRelativePath_from_whole_array() {
        // normally bumping up and traversing an array goes to the element, but if we select the array itself as a
        // starting point, test what happens...

        String queryPath = "$.a.b";
        List<String> matchingPaths = jpathCtx.read(queryPath);

        String sameObject = "@"; // stay in 'b'
        assertEquals(singletonList(ResolvedPaths.of("$['a']['b']", "$['a']['b']", "")),
                resolveTargetPaths(jpathCtx, sameObject, matchingPaths));

        String goUpOne = "@^a"; // go up to 'a'
        assertEquals(singletonList(ResolvedPaths.of("$['a']['b']", "$['a']", "")),
                resolveTargetPaths(jpathCtx, goUpOne, matchingPaths));

        String refIndexAtSameLevel = "@[1]"; // stay in 'b' but specific index
        assertEquals(singletonList(ResolvedPaths.of("$['a']['b']", "$['a']['b'][1]", "")),
                resolveTargetPaths(jpathCtx, refIndexAtSameLevel, matchingPaths));

        String insertionPointAtSameLevel = "@+[+]"; // stay in 'b' but add new item
        List<ResolvedPaths> insertionInfo = resolveTargetPaths(jpathCtx, insertionPointAtSameLevel, matchingPaths);
        assertEquals(singletonList(ResolvedPaths.of("$['a']['b']", "$['a']['b']", "[+]")),
                insertionInfo);

        applyUpdatePath(jpathCtx, insertionInfo.get(0).getTargetBasePath(), insertionInfo.get(0).getTargetUpdatePath(), "whatever");
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
    public void testRelativePath_from_indexed_item_in_array() {
        String queryPath = "$.a.b[0]";
        List<String> matchingPaths = jpathCtx.read(queryPath);

        String sameObject = "@"; // stay in 'b[0]'
        assertEquals(singletonList(ResolvedPaths.of("$['a']['b'][0]", "$['a']['b'][0]", "")),
                resolveTargetPaths(jpathCtx, sameObject, matchingPaths));

        String goUpOneObject = "@^a"; // go up to 'a'
        assertEquals(singletonList(ResolvedPaths.of("$['a']['b'][0]", "$['a']", "")),
                resolveTargetPaths(jpathCtx, goUpOneObject, matchingPaths));


        String insertionPointAtSameLevelShort = "@+[+]"; // same array but change index
        List<ResolvedPaths> insertionInfo = resolveTargetPaths(jpathCtx, insertionPointAtSameLevelShort, matchingPaths);
        assertEquals(singletonList(ResolvedPaths.of("$['a']['b'][0]", "$['a']['b']", "[+]")),
                insertionInfo);

        String insertInSameArray1 = "@^a+b[+]"; // round about way to stay stay in 'b', add new item
        assertEquals(singletonList(
                ResolvedPaths.of("$['a']['b'][0]", "$['a']", "b[+]")),
                resolveTargetPaths(jpathCtx, insertInSameArray1, matchingPaths));

        // failure cases, if in the array item, cannot go to the array itself otherwise this would be ambigious if you
        // wanted to go up to higher object, or wanted to be in array.
        try {
            String goUpOneToArray = "@^b"; // go up to 'b'
            assertEquals(singletonList(ResolvedPaths.of("$['a']['b'][0]", "$['a']['b']", "")),
                    resolveTargetPaths(jpathCtx, goUpOneToArray, matchingPaths));

            fail("Cannot path from array item to its own array");
        } catch (IllegalStateException ex) {
            // yay!
        }

        try {
            String refIndexAtSameLevel = "@^b[1]"; // stay in 'b' then down to specific index
            assertEquals(singletonList(ResolvedPaths.of("$['a']['b'][0]", "$['a']['b'][1]", "")),
                    resolveTargetPaths(jpathCtx, refIndexAtSameLevel, matchingPaths));
            fail("Cannot path from array item to its own array");
        } catch (IllegalStateException ex) {
            // yay!
        }

        try {
            String insertionPointAtSameLevel = "@^b+[+]"; // up to 'b' and add new item
            assertEquals(singletonList(ResolvedPaths.of("$['a']['b'][0]", "$['a']['b']", "[+]")),
                    resolveTargetPaths(jpathCtx, insertionPointAtSameLevel, matchingPaths));
            fail("Cannot path from array item to its own array");
        } catch (IllegalStateException ex) {
            // yay!
        }

        try {
            String insertInSameArray2 = "@^b^a+b[+]"; // more crazy round about way to stay stay in 'b', add new item
            assertEquals(singletonList(
                    ResolvedPaths.of("$['a']['b'][0]", "$['a']", "b[+]")),
                    resolveTargetPaths(jpathCtx, insertInSameArray2, matchingPaths));
            fail("Cannot path from array item to its own array");
        } catch (IllegalStateException ex) {
            // yay!
        }
    }

    @Test
    public void testCasesFailingFromConcatTesting() {
        String queryPath = "$.states[*].name";

        String relativePath = "@^states+cities[*].stateName"; // go up from name to states[x] down to all cities, and add stateName
        String absolutePath = "$.states[*]+cities[*].stateName"; // prefix match into states[x], then down to all cities, and add stateName

        String popTooFar = "@^states";

        List<String> matchingPaths = jpathCtx.read(queryPath);

        assertEquals(Arrays.asList(
                ResolvedPaths.of("$['states'][0]['name']", "$['states'][0]", "cities[*].stateName"),
                ResolvedPaths.of("$['states'][1]['name']", "$['states'][1]", "cities[*].stateName")),
                resolveTargetPaths(jpathCtx, relativePath, matchingPaths));

        assertEquals(Arrays.asList(
                ResolvedPaths.of("$['states'][0]['name']", "$['states'][0]", "cities[*].stateName"),
                ResolvedPaths.of("$['states'][1]['name']", "$['states'][1]", "cities[*].stateName")),
                resolveTargetPaths(jpathCtx, absolutePath, matchingPaths));


        assertEquals(Arrays.asList(
                ResolvedPaths.of("$['states'][0]['name']", "$['states'][0]", ""),
                ResolvedPaths.of("$['states'][1]['name']", "$['states'][1]", "")),
                resolveTargetPaths(jpathCtx, popTooFar, matchingPaths));
    }

    @Test
    public void testUpdates() {
        String queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]";
        List<String> matchingPaths = jpathCtx.read(queryPath);

        String insertionPoint = "@^c^b+somethingNew";
        List<ResolvedPaths> insertionInfo = resolveTargetPaths(jpathCtx, insertionPoint, matchingPaths);
        insertionInfo.forEach(it ->
                applyUpdatePath(jpathCtx, it.getTargetBasePath(), it.getTargetUpdatePath(), "HOWDY!"));

        Object json = jpathCtx.json();
        List<String> read = JsonPath.compile("$.a.b[0].somethingNew").read(json, jvalueListConfig);
        String checkInsert = read.get(0);
        assertEquals("HOWDY!", checkInsert);

        String insertionPointAtSameLevel = "@^c.d+e2";
        List<ResolvedPaths> insertionInfoAtSameLevel = resolveTargetPaths(jpathCtx, insertionPointAtSameLevel, matchingPaths);
        insertionInfoAtSameLevel.forEach(it ->
                applyUpdatePath(jpathCtx, it.getTargetBasePath(), it.getTargetUpdatePath(), "HOWDY!"));


        Object json2 = jpathCtx.json();
        List<String> read2 = JsonPath.compile("$.a.b[0].c.d.e2").read(json2, jvalueListConfig);
        String checkInsert2 = read2.get(0);
        assertEquals("HOWDY!", checkInsert2);


        String replacementPoint = "@^c.d.e";
        List<ResolvedPaths> replacementInfo = resolveTargetPaths(jpathCtx, replacementPoint, matchingPaths);
        replacementInfo.forEach(it ->
                applyUpdatePath(jpathCtx, it.getTargetBasePath(), it.getTargetUpdatePath(), "HOWDY!"));


        Object json3 = jpathCtx.json();
        List<String> read3 = JsonPath.compile("$.a.b[0].c.d.e2").read(json3, jvalueListConfig);
        String checkInsert3 = read3.get(0);
        assertEquals("HOWDY!", checkInsert3);

        String insertionPointNewArray = "@^c^b+newArray[+].value1";
        List<ResolvedPaths> insertionInfoNewArray = resolveTargetPaths(jpathCtx, insertionPointNewArray, matchingPaths);
        insertionInfoNewArray.forEach(it ->
                applyUpdatePath(jpathCtx, it.getTargetBasePath(), it.getTargetUpdatePath(), "HOWDY 1"));

        String insertionPointNewArray2 = "@^c^b+newArray[*].value2";
        List<ResolvedPaths> insertionInfoNewArray2 = resolveTargetPaths(jpathCtx, insertionPointNewArray2, matchingPaths);
        insertionInfoNewArray2.forEach(it ->
                applyUpdatePath(jpathCtx, it.getTargetBasePath(), it.getTargetUpdatePath(), "HOWDY 2"));

        Object json4 = jpathCtx.json();
        List<String> read4 = JsonPath.compile("$.a.b[0].newArray[0].value1").read(json4, jvalueListConfig);
        String checkInsertInNewArray1 = read4.get(0);
        assertEquals("HOWDY 1", checkInsertInNewArray1);

        Object json5 = jpathCtx.json();
        List<String> read5 = JsonPath.compile("$.a.b[0].newArray[0].value2").read(json5, jvalueListConfig);
        String checkInsertInNewArray2 = read5.get(0);
        assertEquals("HOWDY 2", checkInsertInNewArray2);
    }
}
