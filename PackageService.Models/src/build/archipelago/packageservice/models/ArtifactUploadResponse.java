package build.archipelago.packageservice.models;

import lombok.*;

@Builder
@Value
public class ArtifactUploadResponse {
    private String hash;
}
