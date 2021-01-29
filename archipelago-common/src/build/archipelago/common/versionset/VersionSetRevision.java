package build.archipelago.common.versionset;

import build.archipelago.common.ArchipelagoBuiltPackage;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Builder
@Value
public class VersionSetRevision {
    private Instant created;
    private List<ArchipelagoBuiltPackage> packages;
}
