package build.archipelago.harbor.client;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.versionset.*;
import build.archipelago.packageservice.client.models.*;

import java.nio.file.Path;

public interface HarborClient {
    VersionSet getVersionSet(String versionSet) throws VersionSetDoseNotExistsException;
    VersionSetRevision getVersionSetRevision(String versionSetName, String revisionId)
            throws VersionSetDoseNotExistsException;

    Path getBuildArtifact(ArchipelagoBuiltPackage pkg, Path directory) throws PackageNotFoundException, UnauthorizedException;

    void createPackage(CreatePackageRequest request) throws PackageExistsException, UnauthorizedException;
    String getConfig(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException;
    GetPackageResponse getPackage(String pkg) throws PackageNotFoundException;
}
