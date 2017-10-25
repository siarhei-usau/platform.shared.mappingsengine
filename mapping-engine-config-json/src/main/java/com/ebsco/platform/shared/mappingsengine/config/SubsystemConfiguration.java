package com.ebsco.platform.shared.mappingsengine.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class SubsystemConfiguration {
    @NonNull
    private Xml2JsonConfig xml2json;
}

