package com.ebsco.entarch.mappings.streamsets;

import com.streamsets.pipeline.api.ErrorCode;
import com.streamsets.pipeline.api.GenerateResourceBundle;

@GenerateResourceBundle
public enum Errors implements ErrorCode {
    EBSCO_INVALID_CONFIG("Configuration is invalid because of {}"),
    EBSCO_RECORD_ERROR("Record transformation failed due to: {}");

    private String msg;
    Errors(String msg) {
        this.msg = msg;
    }

    @Override
    public String getCode() {
        return this.name();
    }

    @Override
    public String getMessage() {
        return msg;
    }
}
