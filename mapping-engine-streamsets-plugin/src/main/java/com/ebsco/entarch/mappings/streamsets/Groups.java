package com.ebsco.entarch.mappings.streamsets;

import com.streamsets.pipeline.api.GenerateResourceBundle;
import com.streamsets.pipeline.api.Label;

@GenerateResourceBundle
public enum Groups implements Label {
    MAPPINGS("Mappings");

    private String groupName;

    Groups(String groupName) {
        this.groupName = groupName;
    }

    public String getLabel() {
        return groupName;
    }
}
