package build.archipelago.buildserver.models.rest;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class BuildsRestResponse {
    private List<BuildRestResponse> processingBuilds;
    private List<BuildRestResponse> waitingBuilds;
    private List<BuildRestResponse> pastBuilds;
}
