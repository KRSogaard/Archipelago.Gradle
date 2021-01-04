package build.archipelago.packageservice.core.delegates.uploadBuildArtifact;

import build.archipelago.common.ArchipelagoPackage;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UploadBuildArtifactDelegateRequest {
    private String accountId;
    private ArchipelagoPackage pkg;
    private String config;
    private String gitCommit;
    private String gitBranch;
    private byte[] buildArtifact;

    protected void validate() {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id required");
        Preconditions.checkNotNull(pkg, "Name required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(config), "Config required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(gitCommit), "Config required");
        Preconditions.checkNotNull(buildArtifact, "Build artifact required");
    }
}
