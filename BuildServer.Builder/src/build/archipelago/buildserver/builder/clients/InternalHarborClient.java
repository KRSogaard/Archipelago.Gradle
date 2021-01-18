package build.archipelago.buildserver.builder.clients;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.versionset.*;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.harbor.client.models.CreatePackageRequest;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.exceptions.*;
import build.archipelago.packageservice.models.*;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;

import java.nio.file.Path;

public class InternalHarborClient implements HarborClient {

    private VersionSetServiceClient versionSetServiceClient;
    private PackageServiceClient packageServiceClient;
    private String accountId;

    public InternalHarborClient(
            VersionSetServiceClient versionSetServiceClient,
            PackageServiceClient packageServiceClient,
            String accountId) {
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
    public GetBuildArtifactResponse getBuildArtifact(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException {
        return packageServiceClient.getBuildArtifact(accountId, pkg);
    }

    @Override
    public void createPackage(CreatePackageRequest request) throws PackageExistsException {
        packageServiceClient.createPackage(accountId,
                build.archipelago.packageservice.client.models.CreatePackageRequest.builder()
                        .name(request.getName())
                        .description(request.getDescription())
                        .build());
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
