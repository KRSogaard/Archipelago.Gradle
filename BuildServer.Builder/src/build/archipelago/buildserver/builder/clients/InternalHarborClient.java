package build.archipelago.buildserver.builder.clients;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.exceptions.PackageExistsException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.CreatePackageRequest;
import build.archipelago.packageservice.models.PackageDetails;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;

import java.nio.file.Path;

public class InternalHarborClient implements HarborClient {

    private VersionSetServiceClient versionSetServiceClient;
    private PackageServiceClient packageServiceClient;
    private String accountId;

    public InternalHarborClient(
            VersionSetServiceClient versionSetServiceClient,
            PackageServiceClient packageServiceClient,
            String accountId)
    {
        this.versionSetServiceClient = versionSetServiceClient;
        this.packageServiceClient = packageServiceClient;
        this.accountId = accountId;
    }

    @Override
    public VersionSet getVersionSet(String versionSet) throws VersionSetDoseNotExistsException {
        return versionSetServiceClient.getVersionSet(accountId, versionSet);
    }

    @Override
    public VersionSetRevision getVersionSetRevision(String versionSetName, String revisionId) throws VersionSetDoseNotExistsException {
        return versionSetServiceClient.getVersionSetPackages(accountId, versionSetName, revisionId);
    }

    @Override
    public Path getBuildArtifact(ArchipelagoBuiltPackage pkg, Path directory) throws PackageNotFoundException, UnauthorizedException {
        return packageServiceClient.getBuildArtifact(accountId, pkg, directory);
    }

    @Override
    public void createPackage(CreatePackageRequest request) throws PackageExistsException, UnauthorizedException {
        packageServiceClient.createPackage(accountId, request);
    }

    @Override
    public String getConfig(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException {
        return packageServiceClient.getPackageBuild(accountId, pkg).getConfig();
    }

    @Override
    public PackageDetails getPackage(String pkg) throws PackageNotFoundException {
        return packageServiceClient.getPackage(accountId, pkg);
    }
}
