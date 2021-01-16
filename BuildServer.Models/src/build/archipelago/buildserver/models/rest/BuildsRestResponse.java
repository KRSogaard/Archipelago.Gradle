package build.archipelago.buildserver.models.rest;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BuildsRestResponse {
    private List<BuildRestResponse> processingBuilds;
    private List<BuildRestResponse> waitingBuilds;
    private List<BuildRestResponse> pastBuilds;
}
