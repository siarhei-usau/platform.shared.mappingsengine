package com.ebsco.platform.shared.mappingsengine.core;

import com.ebsco.platform.shared.mappingsengine.core.transformers.*;

import java.util.HashMap;
import java.util.Map;

public class DefaultTransformers {
    public static Map<String, Class<? extends JsonTransformer>> TRANFORMERS = new HashMap<>();

    static {
        TRANFORMERS.put("rename", RenameJson.class);
        TRANFORMERS.put("copy", CopyJson.class);
        TRANFORMERS.put("delete", DeleteJson.class);
        TRANFORMERS.put("concat", OriginalConcatJson.class);
        TRANFORMERS.put("concatBroken", ConcatJson.class);
        TRANFORMERS.put("lookup", LookupJson.class);
        TRANFORMERS.put("insert", InsertJson.class);
        TRANFORMERS.put("pivot", PivotJson.class);
    }
}
