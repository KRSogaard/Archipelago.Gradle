package build.archipelago.packageservice.core.data;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.packageservice.core.data.models.*;
import com.google.common.collect.ImmutableList;

public interface PackageData {
    boolean buildExists(ArchipelagoBuiltPackage pkg);
    boolean packageVersionExists(ArchipelagoPackage pkg);
    boolean packageExists(String name);

    PackageDetails getPackageDetails(String name) throws PackageNotFoundException;
    ImmutableList<VersionBuildDetails> getPackageVersionBuilds(ArchipelagoPackage pkg);
    BuiltPackageDetails getBuildPackage(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException;
    BuiltPackageDetails getLatestBuildPackage(ArchipelagoPackage pkg) throws PackageNotFoundException;

    void createBuild(ArchipelagoBuiltPackage pkg, String config, String gitCommit) throws PackageNotFoundException, PackageExistsException;
    void createPackage(CreatePackageModel model) throws PackageExistsException;
}
