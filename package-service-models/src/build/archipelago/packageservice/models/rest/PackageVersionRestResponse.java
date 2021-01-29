package build.archipelago.packageservice.models.rest;

import build.archipelago.packageservice.models.PackageDetailsVersion;
import lombok.*;

import java.time.Instant;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class PackageVersionRestResponse {
    private String version;
    private String latestBuildHash;
    private long latestBuildTime;

    public String getVersion() {
        return version;
    }

    public String getLatestBuildHash() {
        return latestBuildHash;
    }

    public long getLatestBuildTime() {
        return latestBuildTime;
    }

    public PackageDetailsVersion toInternal() {
        return PackageDetailsVersion.builder()
                .version(this.getVersion())
                .latestBuildHash(this.getLatestBuildHash())
                .latestBuildTime(Instant.ofEpochMilli(this.getLatestBuildTime()))
                .build();
    }

    public static PackageVersionRestResponse from(PackageDetailsVersion version) {
        return PackageVersionRestResponse.builder()
                .version(version.getVersion())
                .latestBuildHash(version.getLatestBuildHash())
                .latestBuildTime(version.getLatestBuildTime().toEpochMilli())
                .build();
    }
}
