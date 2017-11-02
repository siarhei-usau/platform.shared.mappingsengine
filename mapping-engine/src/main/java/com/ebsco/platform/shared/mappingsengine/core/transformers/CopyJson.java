package com.ebsco.platform.shared.mappingsengine.core.transformers;

import com.ebsco.platform.shared.mappingsengine.core.JsonTransformer;
import com.ebsco.platform.shared.mappingsengine.core.JsonTransformerContext;
import com.ebsco.platform.shared.mappingsengine.core.ResolvedPaths;
import com.jayway.jsonpath.JsonPath;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Setter
public class CopyJson implements JsonTransformer {

    private String fromPath;
    private String targetPath;

    public CopyJson(String fromPath, String targetPath) {
        this.fromPath = fromPath;
        this.targetPath = targetPath;
    }

    @Override
    public void apply(JsonTransformerContext context) {
        List<ResolvedPaths> fromToMapping = context.queryAndResolveTargetPaths( JsonPath.compile(fromPath), targetPath);
        fromToMapping.forEach(mapping -> {
            Object sourceValue = context.queryForValue(mapping.getSourcePath());
            context.applyUpdate(mapping, sourceValue);
        });
    }
}
