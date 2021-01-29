package build.archipelago.buildserver.models.rest;

import build.archipelago.buildserver.models.BuildPackageDetails;
import lombok.*;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class BuildPackageRestRequest {
    private String packageName;
    private String commit;

    public static BuildPackageRestRequest from(BuildPackageDetails pkg) {
        return BuildPackageRestRequest.builder()
                .packageName(pkg.getPackageName())
                .commit(pkg.getCommit())
                .build();
    }

    public BuildPackageDetails toInternal() {
        return BuildPackageDetails.builder()
                .packageName(getPackageName())
                .commit(getCommit())
                .build();
    }
}
