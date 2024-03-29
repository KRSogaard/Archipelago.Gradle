package build.archipelago.packageservice.models.rest;

import build.archipelago.packageservice.models.VersionBuildDetails;
import lombok.*;

import java.time.Instant;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class PackageBuildRestResponse {
    private String hash;
    private long created;

    public VersionBuildDetails toInternal() {
        return VersionBuildDetails.builder()
                .hash(this.getHash())
                .created(Instant.ofEpochMilli(this.getCreated()))
                .build();
    }

    public static PackageBuildRestResponse from(VersionBuildDetails pkg) {
        return PackageBuildRestResponse.builder()
                .hash(pkg.getHash())
                .created(pkg.getCreated().toEpochMilli())
                .build();
    }
}
