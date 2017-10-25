package com.ebsco.platform.shared.mappingsengine.core;

import com.ebsco.platform.shared.mappingsengine.core.transformers.*;

import java.util.HashMap;
import java.util.Map;

public class DefaultTransformers {
    public static Map<String, Class<? extends JsonTransformer>> TRANFORMERS = new HashMap<>();

    static {
        TRANFORMERS.put("rename", RenameJsonTransform.class);
        TRANFORMERS.put("copy", CopyJsonTransform.class);
        TRANFORMERS.put("delete", DeleteJsonTransform.class);
        TRANFORMERS.put("concat", ConcatJsonTransform.class);
        TRANFORMERS.put("lookup", LookupJsonTransform.class);
    }
}
