package build.archipelago.buildserver.common.services.build.models;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class BuildRequest {
    private String buildId;
    private String versionSet;
    private boolean dryRun;
    private List<BuildPackageDetails> buildPackages;
    private Instant created;
}
