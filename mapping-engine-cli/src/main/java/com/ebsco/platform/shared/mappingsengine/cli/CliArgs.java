package com.ebsco.platform.shared.mappingsengine.cli;

import com.beust.jcommander.Parameter;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
class CliArgs {
    @Parameter(
            names = {"--config"},
            description = "configuration filename for mappings instructions",
            required = true
    )
    private String configFileName;

    @Parameter(
            names = {"--input"},
            description = "input XML filename",
            required = true
    )
    private String xmlInputFileName;

    @Parameter(
            names = {"--json", "--output"},
            description = "optional output JSON filename",
            required = false
    )
    private String jsonInputFileName;

    @Parameter(
            names = {"--stacktrace"},
            description = "print stack trace on error",
            required = false
    )
    private Boolean stackTrace = false;
}
