package build.archipelago.buildserver.models;

import build.archipelago.common.ArchipelagoPackage;
import lombok.*;

@Builder
@Value
public class PackageBuild {
    private ArchipelagoPackage pkg;
    private boolean direct;
}
