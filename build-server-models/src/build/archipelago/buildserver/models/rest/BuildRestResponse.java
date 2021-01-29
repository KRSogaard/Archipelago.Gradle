package build.archipelago.buildserver.models.rest;

import build.archipelago.buildserver.models.*;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
                .stagePackages(archipelagoBuild.getStagePackages().getStatus())
                .stagePublish(archipelagoBuild.getStagePublish().getStatus())
                .buildPackages(archipelagoBuild.getBuildPackages().stream().map(BuildPackageDetails::toString)
                        .collect(Collectors.toList()))
                .build();
    }

    public ArchipelagoBuild toInternal() {
        return ArchipelagoBuild.builder()
                .buildId(this.getBuildId())
                .accountId(this.getAccountId())
                .versionSet(this.getVersionSet())
                .dryRun(this.isDryRun())
                .created(Instant.ofEpochMilli(this.getCreated()))
                .updated(Instant.ofEpochMilli(this.getUpdated()))
                .buildStatus(BuildStatus.getEnum(this.getBuildStatus()))
                .stagePrepare(BuildStatus.getEnum(this.getStagePrepare()))
                .stagePackages(BuildStatus.getEnum(this.getStagePackages()))
                .stagePublish(BuildStatus.getEnum(this.getStagePublish()))
                .buildPackages(this.getBuildPackages().stream().map(BuildPackageDetails::parse).collect(Collectors.toList()))
                .build();
    }
}
