package build.archipelago.buildserver.models;

import build.archipelago.common.ArchipelagoPackage;
import lombok.*;

import java.time.Instant;

@Builder
@Value
public class PackageBuildStatus {
    private ArchipelagoPackage pkg;
    private boolean direct;
    private Instant created;
    private Instant updated;
    private BuildStatus status;
}
