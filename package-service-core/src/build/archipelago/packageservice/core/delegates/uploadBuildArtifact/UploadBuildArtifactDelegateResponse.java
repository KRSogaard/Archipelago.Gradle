package build.archipelago.packageservice.core.delegates.uploadBuildArtifact;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class UploadBuildArtifactDelegateResponse {
    private String hash;
    private String uploadUrl;
}
