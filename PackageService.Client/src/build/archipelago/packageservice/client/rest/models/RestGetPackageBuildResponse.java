package build.archipelago.packageservice.client.rest.models;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RestGetPackageBuildResponse {
    private String hash;
    private long created;
    private String config;
}
