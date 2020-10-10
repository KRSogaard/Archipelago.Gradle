package build.archipelago.versionsetservice.models;

import lombok.*;

@Builder
@Value
public class CreateVersionSetRevisionResponse {
    private String revisionId;
}
