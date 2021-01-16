package build.archipelago.packageservice.models.rest;

import build.archipelago.packageservice.models.PackageDetailsVersion;
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
                .version(getVersion())
                .latestBuildHash(getLatestBuildHash())
                .latestBuildTime(Instant.ofEpochMilli(getLatestBuildTime()))
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
