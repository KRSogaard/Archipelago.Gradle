package build.archipelago.buildserver.api.client;

import build.archipelago.buildserver.models.BuildPackageDetails;
import build.archipelago.buildserver.models.client.Builds;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;

import java.util.List;

public interface BuildServerAPIClient {
    String startBuild(String accountId, String versionSet, boolean dryRun, List<BuildPackageDetails> packages) throws VersionSetDoseNotExistsException;
    Builds getBuilds(String accountId);
}
