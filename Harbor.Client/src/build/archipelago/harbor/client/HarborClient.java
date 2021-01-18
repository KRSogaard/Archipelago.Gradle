package build.archipelago.harbor.client;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.versionset.*;
import build.archipelago.harbor.client.models.CreatePackageRequest;
import build.archipelago.packageservice.exceptions.*;
import build.archipelago.packageservice.models.*;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;

import java.nio.file.Path;

public interface HarborClient {
    VersionSet getVersionSet(String versionSet) throws VersionSetDoseNotExistsException;

    VersionSetRevision getVersionSetRevision(String versionSetName, String revisionId)
            throws VersionSetDoseNotExistsException;

    Path getBuildArtifact(ArchipelagoBuiltPackage pkg, Path directory) throws PackageNotFoundException;

    GetBuildArtifactResponse getBuildArtifact(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException;

    void createPackage(CreatePackageRequest request) throws PackageExistsException;

    String getConfig(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException;

    PackageDetails getPackage(String pkg) throws PackageNotFoundException;
}
