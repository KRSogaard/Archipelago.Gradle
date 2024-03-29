package build.archipelago.buildserver.models;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class ArchipelagoBuild {
    private String buildId;
    private String accountId;
    private String versionSet;
    private boolean dryRun;
    private List<BuildPackageDetails> buildPackages;
    private Instant created;
    private Instant updated;
    private BuildStatus buildStatus;
    private BuildStatus stagePrepare;
    private BuildStatus stagePackages;
    private BuildStatus stagePublish;
}
