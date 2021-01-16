package build.archipelago.harbor.client;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.exceptions.PackageExistsException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.packageservice.client.models.CreatePackageRequest;
import build.archipelago.packageservice.models.PackageDetails;

import java.nio.file.Path;

public interface HarborClient {
    VersionSet getVersionSet(String versionSet) throws VersionSetDoseNotExistsException;
    VersionSetRevision getVersionSetRevision(String versionSetName, String revisionId)
            throws VersionSetDoseNotExistsException;

    Path getBuildArtifact(ArchipelagoBuiltPackage pkg, Path directory) throws PackageNotFoundException;

    void createPackage(CreatePackageRequest request) throws PackageExistsException;
    String getConfig(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException;
    PackageDetails getPackage(String pkg) throws PackageNotFoundException;
}
