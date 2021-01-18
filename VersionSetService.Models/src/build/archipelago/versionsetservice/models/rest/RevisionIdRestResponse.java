package build.archipelago.versionsetservice.models.rest;

import build.archipelago.common.versionset.Revision;
import lombok.*;

import java.time.Instant;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class RevisionIdRestResponse {
    private String revisionId;
    private Long created;

    public static RevisionIdRestResponse from(Revision r) {
        return RevisionIdRestResponse.builder()
                .revisionId(r.getRevisionId())
                .created(r.getCreated().toEpochMilli())
                .build();
    }

    public Revision toInternal() {
        return Revision.builder()
                .revisionId(this.getRevisionId())
                .created(Instant.ofEpochMilli(this.getCreated()))
                .build();
    }
}
