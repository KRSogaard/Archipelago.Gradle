package build.archipelago.common.versionset;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Builder
@Value
public class VersionSetRevision {
    private Instant created;
    private ArchipelagoPackage target;
    private List<ArchipelagoBuiltPackage> packages;
}
