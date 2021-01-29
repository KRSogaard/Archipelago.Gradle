package build.archipelago.packageservice.core.delegates.getBuildArtifact;

import build.archipelago.common.ArchipelagoBuiltPackage;
import lombok.*;

@Builder
@Data
public class GetBuildArtifactResponse {
    private String downloadUrl;
    private ArchipelagoBuiltPackage pkg;
}
