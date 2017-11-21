package com.ebsco.platform.shared.mappingsengine.core;

import lombok.Value;

@Value
public class ResolvedPaths {
    public String sourcePath;
    public String targetBasePath;
    public String targetUpdatePath;

    public ResolvedPaths(final String sourcePath, final String targetBasePath, final String targetUpdatePath) {
        this.sourcePath = sourcePath;
        this.targetBasePath = targetBasePath;
        this.targetUpdatePath = targetUpdatePath;
    }

    public static ResolvedPaths of(final String sourcePath, final String targetBasePath, final String targetUpdatePath) {
        return new ResolvedPaths(sourcePath, targetBasePath, targetUpdatePath);
    }

    public String component1() { return sourcePath; }
    public String component2() { return targetBasePath; }
    public String component3() { return targetUpdatePath; }
}
