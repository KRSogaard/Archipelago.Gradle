package build.archipelago.buildserver.builder;

import build.archipelago.buildserver.models.rest.ArchipelagoBuild;


public class BuildContext {
    private ArchipelagoBuild buildRequest;

    public BuildContext(ArchipelagoBuild buildRequest) {
        this.buildRequest = buildRequest;
    }

}
