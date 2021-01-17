package build.archipelago.packageservice.client;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.PackageExistsException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.packageservice.client.models.CreatePackageRequest;
import build.archipelago.packageservice.client.models.PackageVerificationResult;
import build.archipelago.packageservice.client.models.UploadPackageRequest;
import build.archipelago.packageservice.models.BuiltPackageDetails;
import build.archipelago.packageservice.models.PackageDetails;
import build.archipelago.packageservice.models.VersionBuildDetails;
import com.google.common.collect.ImmutableList;

import java.nio.file.Path;
import java.util.List;

public interface PackageServiceClient {
    void createPackage(String accountId, CreatePackageRequest request) throws PackageExistsException, UnauthorizedException;

    PackageDetails getPackage(String accountId, String name) throws PackageNotFoundException, UnauthorizedException;
    ImmutableList<VersionBuildDetails> getPackageBuilds(String accountId, ArchipelagoPackage pks) throws PackageNotFoundException, UnauthorizedException;
    BuiltPackageDetails getPackageBuild(String accountId, ArchipelagoBuiltPackage pkg) throws PackageNotFoundException, UnauthorizedException;
    ArchipelagoBuiltPackage getPackageByGit(String accountId, String packageName, String commit) throws PackageNotFoundException, UnauthorizedException;

    PackageVerificationResult<ArchipelagoPackage> verifyPackagesExists(String accountId, List<ArchipelagoPackage> packages) throws UnauthorizedException;
    PackageVerificationResult<ArchipelagoBuiltPackage> verifyBuildsExists(String accountId, List<ArchipelagoBuiltPackage> packages) throws UnauthorizedException;

    String uploadBuiltArtifact(String accountId, UploadPackageRequest request, Path file) throws PackageNotFoundException, UnauthorizedException;
    Path getBuildArtifact(String accountId, ArchipelagoBuiltPackage pkg, Path directory) throws PackageNotFoundException, UnauthorizedException;
    ImmutableList<PackageDetails> getAllPackages(String accountId) throws UnauthorizedException;
}
