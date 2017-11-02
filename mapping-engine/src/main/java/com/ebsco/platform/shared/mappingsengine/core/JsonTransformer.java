package com.ebsco.platform.shared.mappingsengine.core;

@FunctionalInterface
public interface JsonTransformer {
    void apply(JsonTransformerContext context);
}
