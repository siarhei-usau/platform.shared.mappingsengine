package com.ebsco.platform.shared.mappingsengine.core;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface JsonTransformer {
    void apply(@NotNull final JsonTransformerContext context);
}
