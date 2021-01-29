package build.archipelago.packageservice.models;

import lombok.*;

@Value
@Builder
public class GetBuildArtifactResponse {
    private String url;
    private String fileName;
    private String hash;
}
