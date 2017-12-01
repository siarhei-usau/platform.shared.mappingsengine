package com.ebsco.platform.shared.mappingsengine.core;

import com.ebsco.platform.shared.mappingsengine.core.transformers.*;
import com.ebsco.platform.shared.mappingsengine.core.JsonTransformerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class TransformsTest extends BasePathTest {

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

    @Test
    @Ignore
    public void testLookupTransformer() throws Exception {
        JsonTransformerContext context = makeContext();
        List<LookupJson.LookupFilter> filters = new ArrayList<>();
        filters.add(new LookupJson.LookupFilter("state", "$.states[*].name"));
        filters.add(new LookupJson.LookupFilter("city", "$.states[*].city[*].name"));
        filters.add(new LookupJson.LookupFilter("active", Collections.singletonList("true")));


        new LookupJson("classpath:/lookup-test.tsv", filters, LookupJson.LookupApplyModes.merge, "$.states[*].city[*]",
                new ObjectMapper().readValue("{ \"population\": \"{{population}}\" }", Map.class)).apply(context);

        assertEquals("Denver", context.queryForValue("$.states[0].cities[0].name"));
        assertEquals("3500000", context.queryForValue("$.states[0].cities[0].population"));
        assertEquals("Boulder", context.queryForValue("$.states[0].cities[1].name"));
        assertEquals("509490", context.queryForValue("$.states[0].cities[1].population"));

        assertEquals("San Francisco", context.queryForValue("$.states[1].cities[0].name"));
        assertEquals("Santa Cruz", context.queryForValue("$.states[1].cities[1].name"));
    }

    @Test
    public void testInsertTransformer() throws Exception {
        JsonTransformerContext context = makeContext();

        Object fragment = new ObjectMapper().readValue("[{\"one\":\"test1\"},{\"two\":\"test2\"}]", List.class);
        new InsertJson("$+testInsert", fragment).apply(context);
        printJson(context.getJsonObject(), "Pretty JSON");

        // a new node have been inserted
        assertEquals(fragment, context.queryForValue(".testInsert"));
    }

    @Test
    public void testPivotTransformer() throws Exception {
        JsonTransformerContext context = makeContext();

        String sourcePath = "$.people[0]";
        String targetPath = "$+pivots";
        String keyField = "firstname";
        String valueField = "lastname";

        new PivotJson(sourcePath, targetPath, keyField, valueField).apply(context);
        printJson(context.getJsonObject(), "Pivot transform Pretty JSON");

        assertEquals("Smith", context.queryForValue("$.pivots.David"));
    }

    @Test
    public void testMultiselectTramsformer()  throws Exception {
        JsonTransformerContext context = makeContext();
//        Object fragment = new ObjectMapper().readValue("[{\"contrib-id-string\": \"38787138\"}]", List.class);
        new MultiSelectJMESJson("$.contrib", "$+contributors", "{rid: [].\"rid\" | [0], ctype: [].\"contrib-id-string\" | [0], name: [].\"given-names\" | [0]}").apply(context);
        printJson(context.getJsonObject(), "MultiSelectJMESJson");
        Object fragment = new ObjectMapper().readValue("[ {\n" +
                "    \"rid\" : \"\",\n" +
                "    \"ctype\" : \"38787138\",\n" +
                "    \"name\" : \"\"\n" +
                "  }, {\n" +
                "    \"rid\" : 2,\n" +
                "    \"ctype\" : \"\",\n" +
                "    \"name\" : \"Patrick Mark\"\n" +
                "  } ]", ArrayNode.class);
        assertEquals(fragment, context.queryForValue(".contributors"));
    }
}
