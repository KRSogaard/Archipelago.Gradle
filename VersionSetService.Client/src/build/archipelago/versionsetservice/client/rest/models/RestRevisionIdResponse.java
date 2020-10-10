package build.archipelago.versionsetservice.client.rest.models;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestRevisionIdResponse {
    private String revisionId;
    private Long created;
}
