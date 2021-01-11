package build.archipelago.versionsetservice.models;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.versionset.VersionSet;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

@Builder
@Value
public class VersionSetRestResponse {
    private String name;
    private String parent;
    private Long created;
    private String latestRevision;
    private Long latestRevisionCreated;
    private List<String> targets;
    private List<RevisionIdRestResponse> revisions;

    public static VersionSetRestResponse fromVersionSet(VersionSet vs) {
        return VersionSetRestResponse.builder()
                .name(vs.getName())
                .created(vs.getCreated().toEpochMilli())
                .parent(vs.getParent())
                .targets(vs.getTargets().stream().map(ArchipelagoPackage::toString).collect(Collectors.toList()))
                .revisions(vs.getRevisions().stream().map(RevisionIdRestResponse::from).collect(Collectors.toList()))
                .latestRevision(vs.getLatestRevision())
                .latestRevisionCreated(
                        vs.getLatestRevisionCreated() != null ? vs.getLatestRevisionCreated().toEpochMilli() : null)
                .build();
    }
}
