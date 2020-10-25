package build.archipelago.packageservice.client;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.packageservice.client.models.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface PackageServiceClient {
    void createPackage(String accountId, CreatePackageRequest request) throws PackageExistsException, UnauthorizedException;

    GetPackageResponse getPackage(String accountId, String name) throws PackageNotFoundException, UnauthorizedException;
    PackageBuildsResponse getPackageBuilds(String accountId, ArchipelagoPackage pks) throws PackageNotFoundException, UnauthorizedException;
    GetPackageBuildResponse getPackageBuild(String accountId, ArchipelagoBuiltPackage pkg) throws PackageNotFoundException, UnauthorizedException;
    ArchipelagoBuiltPackage getPackageByGit(String accountId, String packageName, String branch, String commit) throws PackageNotFoundException, UnauthorizedException;

    PackageVerificationResult<ArchipelagoPackage> verifyPackagesExists(String accountId, List<ArchipelagoPackage> packages) throws UnauthorizedException;
    PackageVerificationResult<ArchipelagoBuiltPackage> verifyBuildsExists(String accountId, List<ArchipelagoBuiltPackage> packages) throws UnauthorizedException;

    String uploadBuiltArtifact(String accountId, UploadPackageRequest request, Path file) throws PackageNotFoundException, UnauthorizedException;
    Path getBuildArtifact(String accountId, ArchipelagoBuiltPackage pkg, Path directory) throws PackageNotFoundException, IOException, UnauthorizedException;
}
