package build.archipelago.packageservice.models;

import lombok.*;

import java.time.Instant;

@Value
@Builder
public class PackageDetailsVersion {
    private String version;
    private String latestBuildHash;
    private Instant latestBuildTime;
}
