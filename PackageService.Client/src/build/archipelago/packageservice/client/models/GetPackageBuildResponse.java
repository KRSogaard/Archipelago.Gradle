package build.archipelago.packageservice.client.models;

import lombok.*;

import java.time.Instant;

@Value
@Builder
public class GetPackageBuildResponse {
    private String hash;
    private Instant created;
    private String config;
}
