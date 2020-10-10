package build.archipelago.versionsetservice.client.rest.models;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestVersionSetResponse {
    private String name;
    private String parent;
    private Long created;
    private String latestRevision;
    private Long latestRevisionCreated;
    private List<String> targets;
    private List<RestRevisionIdResponse> revisions;
}
