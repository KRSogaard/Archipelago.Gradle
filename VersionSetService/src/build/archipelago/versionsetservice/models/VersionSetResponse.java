package build.archipelago.versionsetservice.models;

import lombok.Builder;
import lombok.Value;
import java.util.List;

@Builder
@Value
public class VersionSetResponse {
    private String name;
    private String parent;
    private Long created;
    private String latestRevision;
    private Long latestRevisionCreated;
    private List<String> targets;
    private List<RevisionIdResponse> revisions;
}
