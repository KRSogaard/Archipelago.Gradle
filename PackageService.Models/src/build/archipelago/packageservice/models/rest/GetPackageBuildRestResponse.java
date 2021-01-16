package build.archipelago.packageservice.models.rest;

import build.archipelago.packageservice.models.BuiltPackageDetails;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
                .hash(getHash())
                .created(Instant.ofEpochMilli(getCreated()))
                .config(getConfig())
                .gitCommit(getGitCommit())
                .gitBranch(getGitBranch())
                .build();
    }
}
