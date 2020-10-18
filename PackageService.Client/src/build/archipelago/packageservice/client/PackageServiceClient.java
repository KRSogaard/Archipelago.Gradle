package build.archipelago.packageservice.client;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.packageservice.client.models.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface PackageServiceClient {
    void createPackage(String accountId, CreatePackageRequest request) throws PackageExistsException;

    GetPackageResponse getPackage(String accountId, String name) throws PackageNotFoundException;
    PackageBuildsResponse getPackageBuilds(String accountId, ArchipelagoPackage pks) throws PackageNotFoundException;
    GetPackageBuildResponse getPackageBuild(String accountId, ArchipelagoBuiltPackage pkg) throws PackageNotFoundException;
    ArchipelagoBuiltPackage getPackageByGit(String accountId, String packageName, String branch, String commit) throws PackageNotFoundException;

    PackageVerificationResult<ArchipelagoPackage> verifyPackagesExists(String accountId, List<ArchipelagoPackage> packages);
    PackageVerificationResult<ArchipelagoBuiltPackage> verifyBuildsExists(String accountId, List<ArchipelagoBuiltPackage> packages);

    String uploadBuiltArtifact(String accountId, UploadPackageRequest request, Path file) throws PackageNotFoundException;
    Path getBuildArtifact(String accountId, ArchipelagoBuiltPackage pkg, Path directory) throws PackageNotFoundException, IOException;
}
