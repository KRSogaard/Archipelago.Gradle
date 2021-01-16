package build.archipelago.packageservice.models;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class VersionBuildDetails {
    private String hash;
    private Instant created;
}
