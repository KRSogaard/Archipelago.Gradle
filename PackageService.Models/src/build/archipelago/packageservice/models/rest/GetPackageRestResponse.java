package build.archipelago.packageservice.models.rest;

import build.archipelago.packageservice.models.PackageDetails;
import com.google.common.collect.ImmutableList;
import lombok.*;

import java.time.Instant;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class GetPackageRestResponse {
    private String name;
    private String description;
    private String gitCloneUrl;
    private String gitUrl;
    private String gitRepoName;
    private String gitRepoFullName;
    private long created;
    private List<PackageVersionRestResponse> versions;

    public PackageDetails toInternal() {
        return PackageDetails.builder()
                .name(this.getName())
                .description(this.getDescription())
                .gitCloneUrl(this.getGitCloneUrl())
                .gitUrl(this.getGitUrl())
                .gitRepoName(this.getGitRepoName())
                .gitRepoFullName(this.getGitRepoFullName())
                .created(Instant.ofEpochMilli(this.getCreated()))
                .versions(this.getVersions().stream().map(PackageVersionRestResponse::toInternal)
                        .collect(ImmutableList.toImmutableList()))
                .build();
    }

    public static GetPackageRestResponse from(PackageDetails pkg) {
        return GetPackageRestResponse.builder()
                .name(pkg.getName())
                .description(pkg.getDescription())
                .gitCloneUrl(pkg.getGitCloneUrl())
                .gitUrl(pkg.getGitUrl())
                .gitRepoName(pkg.getGitRepoName())
                .gitRepoFullName(pkg.getGitRepoFullName())
                .created(pkg.getCreated().toEpochMilli())
                .versions(pkg.getVersions().stream().map(PackageVersionRestResponse::from).collect(toList()))
                .build();
    }
}
