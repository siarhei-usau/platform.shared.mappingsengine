package com.ebsco.platform.shared.mappingsengine.core;

import lombok.Value;

@Value(staticConstructor = "of")
public class ResolvedPaths {
    String sourcePath;
    String targetBasePath;
    String targetUpdatePath;
}
