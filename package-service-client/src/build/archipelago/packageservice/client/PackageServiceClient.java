package build.archipelago.packageservice.client;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.packageservice.client.models.*;
import build.archipelago.packageservice.exceptions.*;
import build.archipelago.packageservice.models.*;
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

    GetBuildArtifactResponse getBuildArtifact(String accountId, ArchipelagoBuiltPackage pkg) throws PackageNotFoundException, UnauthorizedException;

    ImmutableList<PackageDetails> getAllPackages(String accountId) throws UnauthorizedException;
}
