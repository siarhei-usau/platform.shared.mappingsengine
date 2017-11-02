package com.ebsco.platform.shared.mappingsengine.core;

import com.ebsco.platform.shared.mappingsengine.core.transformers.ConcatJson;
import com.ebsco.platform.shared.mappingsengine.core.transformers.CopyJson;
import com.ebsco.platform.shared.mappingsengine.core.transformers.DeleteJson;
import com.ebsco.platform.shared.mappingsengine.core.JsonTransformerContext;
import com.ebsco.platform.shared.mappingsengine.core.transformers.RenameJson;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class TransformsTest extends BasePathTest {

    private JsonTransformerContext makeContext() {
        return new JsonTransformerContext(jpathCtx.json(), jpathConfig, jvalueConfig, jvalueListConfig);
    }

    @Test
    public void testRenameTransform() throws Exception {
        String queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]";
        String checkPath = "$.a.b[*].c.renamed[?(@.e == 'foo')]";
        String safePath = "$.a.b[*].c.d[?(@.e == 'miss')]";

        // test move `a.b.c.d[e == 'foo']` to `a.b.c.renamed`

        JsonTransformerContext context = makeContext();

        // value is there before in the source, but not target position
        assertEquals(singletonList("$['a']['b'][0]['c']['d']"), context.queryForPaths(queryPath));
        assertEquals(emptyList(), context.queryForPaths(checkPath));

        // and this one is the safety path that should not be touched
        assertEquals(singletonList("$['a']['b'][1]['c']['d']"), context.queryForPaths(safePath));

        new RenameJson(queryPath, "@^c+renamed").apply(context);
        printJson(context.getJsonObject(), "Pretty JSON");

        // value is in new path but not old
        assertEquals(emptyList(), context.queryForPaths(queryPath));
        assertEquals(singletonList("$['a']['b'][0]['c']['renamed']"), context.queryForPaths(checkPath));

        // and this one is the safety path that should not be touched
        assertEquals(singletonList("$['a']['b'][1]['c']['d']"), context.queryForPaths(safePath));
    }

    @Test
    public void testCopyTransform_SingleValue() throws Exception {
        String queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]";
        String checkPath = "$.a.b[*].c.renamed[?(@.e == 'foo')]";
        String safePath = "$.a.b[*].c.d[?(@.e == 'miss')]";

        // test move `a.b.c.d[e == 'foo']` to `a.b.c.renamed`

        JsonTransformerContext context = makeContext();

        // value is there before in the source, but not target position
        assertEquals(singletonList("$['a']['b'][0]['c']['d']"), context.queryForPaths(queryPath));
        assertEquals(emptyList(), context.queryForPaths(checkPath));

        // and this one is the safety path that should not be touched
        assertEquals(singletonList("$['a']['b'][1]['c']['d']"), context.queryForPaths(safePath));

        new CopyJson(queryPath, "@^c+renamed").apply(context);
        printJson(context.getJsonObject(), "Pretty JSON");

        // value is in new path AND in the old
        assertEquals(singletonList("$['a']['b'][0]['c']['d']"), context.queryForPaths(queryPath));
        assertEquals(singletonList("$['a']['b'][0]['c']['renamed']"), context.queryForPaths(checkPath));

        // and this one is the safety path that should not be touched
        assertEquals(singletonList("$['a']['b'][1]['c']['d']"), context.queryForPaths(safePath));
    }

    @Test
    public void testCopyTransform_TargetIntoArray() throws Exception {
        String queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]";
        String checkPath = "$.a.b[*].c.copied[?(@.e == 'foo')]";
        String checkPath2 = "$.a.b[*].c.copied[?(@.e == 'miss')]";
        String safePath = "$.a.b[*].c.d[?(@.e == 'miss')]";

        // test move `a.b.c.d[e == 'foo']` to `a.b.c.renamed`

        JsonTransformerContext context = makeContext();

        // value is there before in the source, but not target position
        assertEquals(singletonList("$['a']['b'][0]['c']['d']"), context.queryForPaths(queryPath));
        assertEquals(emptyList(), context.queryForPaths(checkPath));

        // and this one is the safety path that should not be touched
        assertEquals(singletonList("$['a']['b'][1]['c']['d']"), context.queryForPaths(safePath));

        new CopyJson(queryPath, "@^c+copied[+]").apply(context);
        printJson(context.getJsonObject(), "Pretty JSON");

        // value is in new path AND in the old
        assertEquals(singletonList("$['a']['b'][0]['c']['d']"), context.queryForPaths(queryPath));
        assertEquals(singletonList("$['a']['b'][0]['c']['copied'][0]"), context.queryForPaths(checkPath));

        // and this one is the safety path that should not be touched
        assertEquals(singletonList("$['a']['b'][1]['c']['d']"), context.queryForPaths(safePath));

        // now copy the safety value over to a whole other part of the tree to go with the other copied value
        new CopyJson(safePath, "@^c^b^a.b[0].c+copied[+]").apply(context);
        printJson(context.getJsonObject(), "Pretty JSON");

        assertEquals(singletonList("$['a']['b'][0]['c']['d']"), context.queryForPaths(queryPath));
        assertEquals(singletonList("$['a']['b'][0]['c']['copied'][0]"), context.queryForPaths(checkPath));

        // our safety value has made it into the array as well!
        assertEquals(singletonList("$['a']['b'][0]['c']['copied'][1]"), context.queryForPaths(checkPath2));
    }

    @Test
    public void testDeleteTransform() throws Exception {
        String queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]";
        JsonTransformerContext context = makeContext();
        assertEquals(singletonList("$['a']['b'][0]['c']['d']"), context.queryForPaths(queryPath));

        new DeleteJson(queryPath).apply(context);
        printJson(context.getJsonObject(), "Pretty JSON");

        // values are gone!
        assertEquals(emptyList(), context.queryForPaths(queryPath));
    }

    @Test
    public void testConcatTransform_InSameObjects() throws Exception {

        JsonTransformerContext context = makeContext();

        // contact firstname and lastname within same objects

        List<String> fromPaths = Arrays.asList("$.people[*].lastname", "$.people[*].firstname");
        new ConcatJson(fromPaths, "$.people[*]+fullname", ", ").apply(context);
        printJson(context.getJsonObject(), "Pretty JSON");

        assertEquals("Smith, David", context.queryForValue("$.people[0].fullname"));
        assertEquals("Stark, Michael", context.queryForValue("$.people[1].fullname"));
    }

    @Test
    public void testConcatTransform_MixedObjectConcat() throws Exception {

        JsonTransformerContext context = makeContext();

        /*
            from { "states": [{ "name": "Colorado", "cities": [{ "name": "Denver" }, { "name": "Boulder" }] , ...] }
            to   { "states": [{ "name": "Colorado", "cities": [{ "name": "Denver", "cityState": "Denver, Colorado" },
                                                               { "name": "Boulder", "cityState": "Boulder, Colorado" }] , ...] }
         */


        // TODO: this is failing until TestPathUtils all pass, one case is broken there, see testCasesFailingFromConcatTesting()

        new CopyJson("$.states[*].name", "@^states+cities[*].stateName").apply(context);
        printJson(context.getJsonObject(), "After Copy");

        List<String> fromPaths = Arrays.asList("$.states[*].cities[*].name", "$.states[*].cities[*].stateName");
        new ConcatJson(fromPaths, "$.states[*].cities[*]+cityState", ", ").apply(context);
        printJson(context.getJsonObject(), "After Concat");

        new DeleteJson("$.states[*].cities[*].stateName").apply(context);
        printJson(context.getJsonObject(), "After Delete");

        assertEquals("Denver, Colorado", context.queryForValue("$.states[0].cities[0].cityState"));
        assertEquals("Boulder, Colorado", context.queryForValue("$.states[0].cities[1].cityState"));
        assertEquals("San Francisco, California", context.queryForValue("$.states[1].cities[0].cityState"));
        assertEquals("Santa Cruz, California", context.queryForValue("$.states[1].cities[1].cityState"));
    }
}
