package com.ebsco.platform.shared.mappingsengine.core;

import org.jetbrains.annotations.NotNull;

public interface JsonTransformer {
    void apply(@NotNull final JsonTransformerContext context);
}

