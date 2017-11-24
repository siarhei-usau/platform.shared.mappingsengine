package com.ebsco.platform.shared.mappingsengine.core;

import com.ebsco.platform.shared.mappingsengine.core.transformers.CopyJson;
import com.ebsco.platform.shared.mappingsengine.core.transformers.RenameJson;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class GithubIssueTests extends BasePathTest {
    @Test
    public void testIssue10_InsertAtRoot() throws Exception {
        String queryPath = "$.a.b[*].c.d[?(@.e == 'foo')]";
        String checkPath1 = "$.newRootItem.renamedItem1";
        String checkPath2 = "$.newRootItem.renamedItem2";

        // test move `a.b.c.d[e == 'foo']` to `a.b.c.renamed`

        JsonTransformerContext context = makeContext();

        // value is there before in the source, but not target position
        assertEquals(singletonList("$['a']['b'][0]['c']['d']"), context.queryForPaths(queryPath));
        assertEquals(emptyList(), context.queryForPaths(checkPath1));
        assertEquals(emptyList(), context.queryForPaths(checkPath2));

        new CopyJson(queryPath, "$+newRootItem.renamedItem1").apply(context);
        new CopyJson(queryPath, "$+newRootItem.renamedItem2").apply(context);
        printJson(context.getJsonObject(), "Pretty JSON");

        assertEquals(singletonList("$['newRootItem']['renamedItem1']"), context.queryForPaths(checkPath1));
        assertEquals(singletonList("$['newRootItem']['renamedItem2']"), context.queryForPaths(checkPath2));


    }

    @Test
    public void testIssue19_DoubleArraySelectionWithTopInsertionFails() throws Exception {
        String queryPath = "$.issue19[*][*]['two']";
        String checkPath1 = "$.newRootItem[*]['two']";

        JsonTransformerContext context = makeContext();

        // value is there before in the source, but not target position
        List<String> expectedNodes = new ArrayList<>();
        expectedNodes.add("$['issue19'][0][1]['two']");
        expectedNodes.add("$['issue19'][1][1]['two']");
        expectedNodes.add("$['issue19'][2][1]['two']");
        assertEquals(expectedNodes, context.queryForPaths(queryPath));
        assertEquals(emptyList(), context.queryForPaths(checkPath1));

        new RenameJson(queryPath, "$+newRootItem[+].two").apply(context);
        printJson(context.getJsonObject(), "Pretty JSON");

        // old nodes should be gone
        assertEquals(emptyList(), context.queryForPaths(queryPath));
        // new nodes should be present
        expectedNodes = new ArrayList<>();
        expectedNodes.add("$['newRootItem'][0]['two']");
        expectedNodes.add("$['newRootItem'][1]['two']");
        expectedNodes.add("$['newRootItem'][2]['two']");

        assertEquals(expectedNodes, context.queryForPaths(checkPath1));

    }
}
