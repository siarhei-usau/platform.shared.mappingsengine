package com.ebsco.platform.shared.mappingsengine.core.transformers;

import com.ebsco.platform.shared.mappingsengine.core.JsonTransformer;
import com.ebsco.platform.shared.mappingsengine.core.JsonTransformerContext;
import com.jayway.jsonpath.JsonPath;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
public class DeleteJson implements JsonTransformer {

    private String deletePath;

    public DeleteJson(String deletePath) {
        this.deletePath = deletePath;
    }

    @Override
    public void apply(JsonTransformerContext context) {
        context.queryForPaths(JsonPath.compile(deletePath))
                .forEach(context::deleteValue);
    }
}
