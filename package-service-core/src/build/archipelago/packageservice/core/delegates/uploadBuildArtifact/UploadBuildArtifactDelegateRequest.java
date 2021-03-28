package build.archipelago.packageservice.core.delegates.uploadBuildArtifact;

import build.archipelago.common.ArchipelagoPackage;
import com.google.common.base.*;
import lombok.*;

@Builder
@Value
public class UploadBuildArtifactDelegateRequest {
    private String accountId;
    private ArchipelagoPackage pkg;
    private String config;
    private String gitCommit;

    protected void validate() {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id required");
        Preconditions.checkNotNull(pkg, "Name required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(config), "Config required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(gitCommit), "Git commit required");
    }
}
