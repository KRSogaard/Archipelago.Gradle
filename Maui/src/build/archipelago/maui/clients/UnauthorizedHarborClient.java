package build.archipelago.maui.clients;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.exceptions.PackageExistsException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.packageservice.client.models.CreatePackageRequest;

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
    public build.archipelago.packageservice.models.PackageDetails getPackage(String pkg) throws PackageNotFoundException {
        throw new UnauthorizedException();
    }
}
