package build.archipelago.packageservice.models.rest;

import build.archipelago.packageservice.models.VersionBuildDetails;
import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class GetPackageBuildsRestResponse {
    private List<PackageBuildRestResponse> builds;

    public static GetPackageBuildsRestResponse from(ImmutableList<VersionBuildDetails> builds) {
        return GetPackageBuildsRestResponse.builder()
                .builds(builds.stream()
                    .map(PackageBuildRestResponse::from)
                    .collect(Collectors.toList()))
                .build();
    }

    public ImmutableList<VersionBuildDetails> toInternal() {
        return getBuilds().stream().map(PackageBuildRestResponse::toInternal)
                .collect(ImmutableList.toImmutableList());
    }
}
