package build.archipelago.versionsetservice.client;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.exceptions.MissingTargetPackageException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.exceptions.VersionSetExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.versionsetservice.client.model.CreateVersionSetRequest;

import java.util.List;

public interface VersionSetServiceClient {
    void createVersionSet(CreateVersionSetRequest request)
            throws VersionSetExistsException, VersionSetDoseNotExistsException, PackageNotFoundException;
    String createVersionRevision(String versionSetName, List<ArchipelagoBuiltPackage> packages)
            throws VersionSetDoseNotExistsException, MissingTargetPackageException, PackageNotFoundException;
    VersionSet getVersionSet(String versionSetName)
            throws VersionSetDoseNotExistsException;
    VersionSetRevision getVersionSetPackages(String versionSetName, String revisionId)
            throws VersionSetDoseNotExistsException;
}
