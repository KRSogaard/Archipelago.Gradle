package build.archipelago.packageservice.models.rest;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArtifactUploadRestRequest {
    private String gitCommit;
    private String config;
}
