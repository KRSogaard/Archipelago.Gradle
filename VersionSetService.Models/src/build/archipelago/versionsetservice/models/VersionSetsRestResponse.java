package build.archipelago.versionsetservice.models;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class VersionSetsRestResponse {
    private List<VersionSetRestResponse> versionSets;
}
