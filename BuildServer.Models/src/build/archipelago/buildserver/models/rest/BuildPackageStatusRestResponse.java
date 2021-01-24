package build.archipelago.buildserver.models.rest;

import build.archipelago.buildserver.models.*;
import build.archipelago.common.ArchipelagoPackage;
import lombok.*;

import java.time.Instant;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class BuildPackageStatusRestResponse {
    private String pkg;
    private boolean direct;
    private long created;
    private long updated;
    private String status;

    public static BuildPackageStatusRestResponse from(PackageBuildStatus packageStatus) {
        return BuildPackageStatusRestResponse.builder()
                .pkg(packageStatus.getPkg().getNameVersion())
                .direct(packageStatus.isDirect())
                .created(packageStatus.getCreated().toEpochMilli())
                .updated(packageStatus.getUpdated().toEpochMilli())
                .status(packageStatus.getStatus().getStatus())
                .build();
    }

    public PackageBuildStatus toInternal() {
        return PackageBuildStatus.builder()
                .pkg(ArchipelagoPackage.parse(this.getPkg()))
                .direct(this.isDirect())
                .created(Instant.ofEpochMilli(this.getCreated()))
                .updated(Instant.ofEpochMilli(this.getUpdated()))
                .status(BuildStatus.getEnum(this.getStatus()))
                .build();
    }
}
