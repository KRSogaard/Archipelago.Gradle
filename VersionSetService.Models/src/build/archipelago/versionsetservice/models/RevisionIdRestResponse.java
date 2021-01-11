package build.archipelago.versionsetservice.models;

import build.archipelago.common.versionset.Revision;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class RevisionIdRestResponse {
    private String revisionId;
    private Long created;

    public static RevisionIdRestResponse from(Revision r) {
        return RevisionIdRestResponse.builder()
                .revisionId(r.getRevisionId())
                .created(r.getCreated().toEpochMilli())
                .build();
    }
}
