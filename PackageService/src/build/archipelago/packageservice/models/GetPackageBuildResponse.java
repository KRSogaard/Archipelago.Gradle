package build.archipelago.packageservice.models;

import lombok.*;

@Builder
@Value
public class GetPackageBuildResponse {
    private String hash;
    private long created;
    private String config;
}
