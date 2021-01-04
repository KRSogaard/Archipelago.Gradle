package build.archipelago.packageservice.core.data;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.PackageExistsException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.core.data.models.BuiltPackageDetails;
import build.archipelago.packageservice.core.data.models.CreatePackageModel;
import build.archipelago.packageservice.core.data.models.PackageDetails;
import build.archipelago.packageservice.core.data.models.VersionBuildDetails;
import com.google.common.collect.ImmutableList;

public interface PackageData {
    boolean buildExists(String accountId, ArchipelagoBuiltPackage pkg);
    boolean packageVersionExists(String accountId, ArchipelagoPackage pkg);
    boolean packageExists(String accountId, String name);

    PackageDetails getPackageDetails(String accountId, String name) throws PackageNotFoundException;
    ImmutableList<VersionBuildDetails> getPackageVersionBuilds(String accountId, ArchipelagoPackage pkg);
    BuiltPackageDetails getBuildPackage(String accountId, ArchipelagoBuiltPackage pkg) throws PackageNotFoundException;
    ArchipelagoBuiltPackage getBuildPackageByGit(String accountId, String packageName, String branch, String gitCommit) throws PackageNotFoundException;
    BuiltPackageDetails getLatestBuildPackage(String accountId, ArchipelagoPackage pkg) throws PackageNotFoundException;

    void createBuild(String accountId, ArchipelagoBuiltPackage pkg, String config, String gitCommit, String gitBranch) throws PackageNotFoundException, PackageExistsException;
    void createPackage(String accountId, CreatePackageModel model) throws PackageExistsException;

    ImmutableList<PackageDetails> getAllPackages(String accountId);
}
