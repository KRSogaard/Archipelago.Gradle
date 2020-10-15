package build.archipelago.packageservice.core.delegates.uploadBuildArtifact;

import build.archipelago.common.ArchipelagoPackage;
import com.google.common.base.*;
import lombok.*;

@Builder
@Data
public class UploadBuildArtifactDelegateRequest {
    private ArchipelagoPackage pkg;
    private String config;
    private String gitCommit;
    private String gitBranch;
    private byte[] buildArtifact;

    protected void validate() {
        Preconditions.checkNotNull(pkg, "Name required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(config), "Config required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(gitCommit), "Config required");
        Preconditions.checkNotNull(buildArtifact, "Build artifact required");
    }
}
