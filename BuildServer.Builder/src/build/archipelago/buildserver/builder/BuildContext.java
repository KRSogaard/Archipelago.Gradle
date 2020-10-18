package build.archipelago.buildserver.builder;

import build.archipelago.account.common.models.AccountDetails;
import build.archipelago.buildserver.common.services.build.models.ArchipelagoBuild;


public class BuildContext {
    private ArchipelagoBuild buildRequest;

    public BuildContext(ArchipelagoBuild buildRequest) {
        this.buildRequest = buildRequest;
    }

}
