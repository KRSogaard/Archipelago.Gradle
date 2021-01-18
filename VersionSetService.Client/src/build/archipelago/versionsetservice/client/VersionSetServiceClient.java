package build.archipelago.versionsetservice.client;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.versionset.*;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.versionsetservice.client.models.CreateVersionSetRequest;
import build.archipelago.versionsetservice.exceptions.*;

import java.util.List;

public interface VersionSetServiceClient {
    List<VersionSet> getVersionSets(String accountId);

    void createVersionSet(String accountId, CreateVersionSetRequest request)
            throws VersionSetExistsException, VersionSetDoseNotExistsException, PackageNotFoundException;

    String createVersionRevision(String accountId, String versionSetName, List<ArchipelagoBuiltPackage> packages)
            throws VersionSetDoseNotExistsException, MissingTargetPackageException, PackageNotFoundException;

    VersionSet getVersionSet(String accountId, String versionSetName)
            throws VersionSetDoseNotExistsException, UnauthorizedException;

    VersionSetRevision getVersionSetPackages(String accountId, String versionSetName, String revisionId)
            throws VersionSetDoseNotExistsException, UnauthorizedException;
}
