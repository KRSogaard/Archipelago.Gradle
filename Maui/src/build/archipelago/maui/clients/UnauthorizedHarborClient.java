package build.archipelago.maui.clients;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.versionset.*;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.packageservice.client.models.*;

import java.nio.file.Path;

public class UnauthorizedHarborClient implements HarborClient {
    @Override
    public VersionSet getVersionSet(String versionSet) throws VersionSetDoseNotExistsException {
        throw new UnauthorizedException();
    }

    @Override
    public VersionSetRevision getVersionSetRevision(String versionSetName, String revisionId) throws VersionSetDoseNotExistsException {
        throw new UnauthorizedException();
    }

    @Override
    public Path getBuildArtifact(ArchipelagoBuiltPackage pkg, Path directory) throws PackageNotFoundException {
        throw new UnauthorizedException();
    }

    @Override
    public void createPackage(CreatePackageRequest request) throws PackageExistsException {
        throw new UnauthorizedException();
    }

    @Override
    public String getConfig(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException {
        throw new UnauthorizedException();
    }

    @Override
    public GetPackageResponse getPackage(String pkg) throws PackageNotFoundException {
        throw new UnauthorizedException();
    }
}