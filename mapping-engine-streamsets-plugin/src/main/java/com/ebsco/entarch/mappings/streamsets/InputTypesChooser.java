package com.ebsco.entarch.mappings.streamsets;

import com.streamsets.pipeline.api.base.BaseEnumChooserValues;

public class InputTypesChooser extends BaseEnumChooserValues<InputTypes> {
    public InputTypesChooser() {
        super(new InputTypes[]{InputTypes.XML, InputTypes.JSON});
    }
}
