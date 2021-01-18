package build.archipelago.versionsetservice.models.rest;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.versionset.VersionSet;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
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
                .parent(vs.getParent())
                .created(vs.getCreated().toEpochMilli())
                .latestRevision(vs.getLatestRevision())
                .latestRevisionCreated(
                        vs.getLatestRevisionCreated() != null ? vs.getLatestRevisionCreated().toEpochMilli() : null)
                .targets(vs.getTargets().stream().map(ArchipelagoPackage::toString).collect(Collectors.toList()))
                .revisions(vs.getRevisions() != null ? vs.getRevisions().stream().map(RevisionIdRestResponse::from).collect(Collectors.toList()) : null)
                .build();
    }

    public VersionSet toInternal() {
        return VersionSet.builder()
                .name(this.getName())
                .parent(this.getParent())
                .created(Instant.ofEpochMilli(this.getCreated()))
                .latestRevision(this.getLatestRevision())
                .latestRevisionCreated(this.getLatestRevisionCreated() != null ? Instant.ofEpochMilli(this.getLatestRevisionCreated()) : null)
                .targets(this.getTargets().stream().map(ArchipelagoPackage::parse).collect(Collectors.toList()))
                .revisions(this.getRevisions() != null ? this.getRevisions().stream().map(RevisionIdRestResponse::toInternal).collect(Collectors.toList()) : null)
                .build();
    }
}
