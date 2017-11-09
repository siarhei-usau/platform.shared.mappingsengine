package com.ebsco.entarch.mappings.streamsets;

import com.streamsets.pipeline.api.base.BaseEnumChooserValues;

public class InstructionsSourcesChooser extends BaseEnumChooserValues<InstructionsSources> {
    public InstructionsSourcesChooser() {
        super(InstructionsSources.Inline, InstructionsSources.File, InstructionsSources.Classpath);
    }
}
