package build.archipelago.buildserver.models.client;

import build.archipelago.buildserver.models.ArchipelagoBuild;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class Builds {
    private List<ArchipelagoBuild> waitingBuilds;
    private List<ArchipelagoBuild> processingBuilds;
    private List<ArchipelagoBuild> pastBuilds;
}
