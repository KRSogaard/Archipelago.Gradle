package build.archipelago.buildserver.common.services.build;

import build.archipelago.buildserver.models.*;
import build.archipelago.buildserver.models.exceptions.BuildNotFoundException;
import build.archipelago.common.ArchipelagoPackage;
import com.google.common.collect.ImmutableList;

import java.util.List;

public interface BuildService {
    String addNewBuildRequest(String accountId, String versionSet, boolean dryRun, List<BuildPackageDetails> buildPackages);

    ArchipelagoBuild getBuildRequest(String accountId, String buildId) throws BuildNotFoundException;

    void setBuildStatus(String accountId, String buildId, BuildStage stage, BuildStatus status);

    void setBuildPackages(String buildId, List<PackageBuild> packages);

    void setPackageStatus(String buildId, ArchipelagoPackage pkg, BuildStatus status);

    List<ArchipelagoBuild> getAllBuildsForAccount(String accountId);

    ImmutableList<PackageBuildStatus> getBuildPackages(String accountId, String buildId);
}
