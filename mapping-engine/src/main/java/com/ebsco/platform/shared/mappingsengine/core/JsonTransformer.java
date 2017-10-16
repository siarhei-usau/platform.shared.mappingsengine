package com.ebsco.platform.shared.mappingsengine.core;

import com.jayway.jsonpath.Configuration;

public interface JsonTransformer {
    void apply(Object jsonObject, Configuration jpathConfig, Configuration jvalueConfig);
}
