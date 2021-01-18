package build.archipelago.packageservice.models;

import lombok.*;

import java.time.Instant;

@Value
@Builder
public class BuiltPackageDetails {
    private String hash;
    private Instant created;
    private String config;
    private String gitCommit;
    private String gitBranch;
}
