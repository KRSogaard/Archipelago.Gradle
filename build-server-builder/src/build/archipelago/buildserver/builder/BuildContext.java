package build.archipelago.buildserver.builder;

import build.archipelago.buildserver.models.ArchipelagoBuild;


public class BuildContext {
    private ArchipelagoBuild buildRequest;

    public BuildContext(ArchipelagoBuild buildRequest) {
        this.buildRequest = buildRequest;
    }

}
