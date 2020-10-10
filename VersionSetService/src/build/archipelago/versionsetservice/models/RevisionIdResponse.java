package build.archipelago.versionsetservice.models;

import build.archipelago.common.versionset.Revision;
import lombok.*;

@Builder
@Value
public class RevisionIdResponse {
    private String revisionId;
    private Long created;

    public static RevisionIdResponse from(Revision r) {
        return RevisionIdResponse.builder()
                .revisionId(r.getRevisionId())
                .created(r.getCreated().toEpochMilli())
                .build();
    }
}
