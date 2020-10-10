package build.archipelago.packageservice.client;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.packageservice.client.models.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface PackageServiceClient {
    void createPackage(CreatePackageRequest request) throws PackageExistsException;

    GetPackageResponse getPackage(String name) throws PackageNotFoundException;
    PackageBuildsResponse getPackageBuilds(ArchipelagoPackage pks) throws PackageNotFoundException;
    GetPackageBuildResponse getPackageBuild(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException;

    PackageVerificationResult<ArchipelagoPackage> verifyPackagesExists(List<ArchipelagoPackage> packages);
    PackageVerificationResult<ArchipelagoBuiltPackage> verifyBuildsExists(List<ArchipelagoBuiltPackage> packages);

    String uploadBuiltArtifact(UploadPackageRequest request, Path file) throws PackageNotFoundException;
    Path getBuildArtifact(ArchipelagoBuiltPackage pkg, Path directory) throws PackageNotFoundException, IOException;
}
