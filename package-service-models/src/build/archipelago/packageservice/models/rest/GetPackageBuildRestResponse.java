package build.archipelago.packageservice.models.rest;

import build.archipelago.packageservice.models.BuiltPackageDetails;
import lombok.*;

import java.time.Instant;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class GetPackageBuildRestResponse {
    private String hash;
    private long created;
    private String config;
    private String gitCommit;
    private String gitBranch;

    public static GetPackageBuildRestResponse from(BuiltPackageDetails build) {
        return GetPackageBuildRestResponse.builder()
                .hash(build.getHash())
                .config(build.getConfig())
                .created(build.getCreated().toEpochMilli())
                .gitCommit(build.getGitCommit())
                .gitBranch(build.getGitBranch())
                .build();
    }

    public BuiltPackageDetails toInternal() {
        return BuiltPackageDetails.builder()
                .hash(this.getHash())
                .created(Instant.ofEpochMilli(this.getCreated()))
                .config(this.getConfig())
                .gitCommit(this.getGitCommit())
                .gitBranch(this.getGitBranch())
                .build();
    }
}
