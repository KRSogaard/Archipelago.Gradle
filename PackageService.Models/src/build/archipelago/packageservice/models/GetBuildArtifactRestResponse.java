package build.archipelago.packageservice.models;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class GetBuildArtifactRestResponse {
    private String url;
    private String fileName;
    private String hash;
}
