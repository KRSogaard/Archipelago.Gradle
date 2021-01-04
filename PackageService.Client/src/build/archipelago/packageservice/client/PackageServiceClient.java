package build.archipelago.packageservice.client;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.PackageExistsException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.packageservice.client.models.CreatePackageRequest;
import build.archipelago.packageservice.client.models.GetPackageBuildResponse;
import build.archipelago.packageservice.client.models.GetPackageResponse;
import build.archipelago.packageservice.client.models.GetPackagesResponse;
import build.archipelago.packageservice.client.models.PackageBuildsResponse;
import build.archipelago.packageservice.client.models.PackageVerificationResult;
import build.archipelago.packageservice.client.models.UploadPackageRequest;

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
    Path getBuildArtifact(String accountId, ArchipelagoBuiltPackage pkg, Path directory) throws PackageNotFoundException, UnauthorizedException;
    GetPackagesResponse getAllPackages(String accountId) throws UnauthorizedException;
}
