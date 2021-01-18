package build.archipelago.versionsetservice.models.rest;

import lombok.*;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class CreateVersionSetRevisionRestResponse {
    private String revisionId;
}
