package build.archipelago.buildserver.api.client;

import build.archipelago.buildserver.models.client.Builds;

public interface BuildServerAPIClient {
    Builds getBuilds(String accountId);
}
