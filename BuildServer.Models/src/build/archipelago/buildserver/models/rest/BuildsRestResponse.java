package build.archipelago.buildserver.models.rest;

import lombok.*;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BuildsRestResponse {
    private List<BuildRestResponse> processingBuilds;
    private List<BuildRestResponse> waitingBuilds;
    private List<BuildRestResponse> pastBuilds;
}
