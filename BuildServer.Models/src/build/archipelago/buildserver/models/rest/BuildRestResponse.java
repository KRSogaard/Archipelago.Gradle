package build.archipelago.buildserver.models.rest;

import build.archipelago.buildserver.models.ArchipelagoBuild;
import build.archipelago.buildserver.models.BuildPackageDetails;
import build.archipelago.buildserver.models.BuildStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Value
public class BuildRestResponse {
    private String buildId;
    private String accountId;
    private String versionSet;
    private boolean dryRun;
    private List<String> buildPackages;
    private long created;
    private long updated;
    private String buildStatus;
    private String stagePrepare;
    private String stagePackages;
    private String stagePublish;

    public static BuildRestResponse from(ArchipelagoBuild archipelagoBuild) {
        return BuildRestResponse.builder()
                .buildId(archipelagoBuild.getBuildId())
                .accountId(archipelagoBuild.getAccountId())
                .versionSet(archipelagoBuild.getVersionSet())
                .dryRun(archipelagoBuild.isDryRun())
                .created(archipelagoBuild.getCreated().toEpochMilli())
                .updated(archipelagoBuild.getUpdated().toEpochMilli())
                .buildStatus(archipelagoBuild.getBuildStatus().getStatus())
                .stagePrepare(archipelagoBuild.getStagePrepare().getStatus())
                .stagePublish(archipelagoBuild.getStagePublish().getStatus())
                .stagePackages(archipelagoBuild.getStagePackages().getStatus())
                .buildPackages(archipelagoBuild.getBuildPackages().stream().map(BuildPackageDetails::toString)
                        .collect(Collectors.toList()))
                .build();
    }

    public ArchipelagoBuild toArchipelagoBuild() {
        return ArchipelagoBuild.builder()
                .buildId(getBuildId())
                .accountId(getAccountId())
                .versionSet(getVersionSet())
                .dryRun(isDryRun())
                .created(Instant.ofEpochMilli(getCreated()))
                .updated(Instant.ofEpochMilli(getUpdated()))
                .buildStatus(BuildStatus.getEnum(getBuildStatus()))
                .stagePrepare(BuildStatus.getEnum(getStagePrepare()))
                .stagePublish(BuildStatus.getEnum(getStagePublish()))
                .buildPackages(getBuildPackages().stream().map(BuildPackageDetails::parse).collect(Collectors.toList()))
                .build();
    }
}
