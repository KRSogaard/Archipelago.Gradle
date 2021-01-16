package build.archipelago.buildserver.common.services.build.models;

import build.archipelago.common.ArchipelagoPackage;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class PackageBuild {
    private ArchipelagoPackage pkg;
    private boolean direct;
}
