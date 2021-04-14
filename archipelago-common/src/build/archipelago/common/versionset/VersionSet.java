package build.archipelago.common.versionset;

import build.archipelago.common.ArchipelagoPackage;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Builder
@Value
public class VersionSet {
    private String name;
    private String parent;
    private Instant created;
    private Instant updated;
    private String latestRevision;
    private Instant latestRevisionCreated;
    private ArchipelagoPackage target;
    private List<Revision> revisions;
    private List<VersionSetCallback> callbacks;
}

