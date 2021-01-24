package build.archipelago.buildserver.api.client;

import build.archipelago.buildserver.models.*;
import build.archipelago.buildserver.models.client.Builds;
import build.archipelago.buildserver.models.exceptions.*;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;
import com.google.common.collect.ImmutableList;

import java.util.List;

public interface BuildServerAPIClient {
    String startBuild(String accountId, String versionSet, boolean dryRun, List<BuildPackageDetails> packages) throws VersionSetDoseNotExistsException;

    Builds getBuilds(String accountId);

    ArchipelagoBuild getBuild(String accountId, String buildId) throws BuildNotFoundException;

    ImmutableList<PackageBuildStatus> getBuildPackages(String accountId, String buildId);

    LogFileResponse getStageLog(String accountId, String buildId, BuildStage stage) throws StageLogNotFoundException;

    LogFileResponse getPackageLog(String accountId, String buildId, ArchipelagoPackage pkg) throws PackageLogNotFoundException;
}
