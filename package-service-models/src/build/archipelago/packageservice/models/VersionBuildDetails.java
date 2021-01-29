package build.archipelago.packageservice.models;

import lombok.*;

import java.time.Instant;

@Value
@Builder
public class VersionBuildDetails {
    private String hash;
    private Instant created;
}
