package build.archipelago.common.versionset;

import build.archipelago.common.ArchipelagoPackage;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Builder
@Value
public class VersionSet {
    private String name;
    private String parent;
    private Instant created;
    private String latestRevision;
    private Instant latestRevisionCreated;
    private List<ArchipelagoPackage> targets;
    private List<Revision> revisions;
}

