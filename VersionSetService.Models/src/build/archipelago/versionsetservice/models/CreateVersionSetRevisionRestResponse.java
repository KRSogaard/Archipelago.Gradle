package build.archipelago.versionsetservice.models;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class CreateVersionSetRevisionRestResponse {
    private String revisionId;
}
