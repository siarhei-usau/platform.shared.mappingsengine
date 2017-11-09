package com.ebsco.entarch.mappings.streamsets;

import com.streamsets.pipeline.api.base.BaseEnumChooserValues;

public class AwsCredentialsTypesChooser extends BaseEnumChooserValues<AwsCredentialsTypes> {
    public AwsCredentialsTypesChooser() {
        super(AwsCredentialsTypes.InstanceRole, AwsCredentialsTypes.ContainerRole, AwsCredentialsTypes.Profile, AwsCredentialsTypes.Basic);
    }
}
