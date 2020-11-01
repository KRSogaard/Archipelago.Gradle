package build.archipelago.harbor.client;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.clients.rest.OAuthRestClient;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.versionset.*;
import build.archipelago.packageservice.client.models.*;

import java.nio.file.Path;

public class RestHarborClient extends OAuthRestClient implements HarborClient {
    public RestHarborClient(String baseUrl, String tokenUrl, String oauthToken, String audience) {
        super(baseUrl, tokenUrl, oauthToken, audience);
    }

    @Override
    public VersionSet getVersionSet(String versionSet) throws VersionSetDoseNotExistsException {
        return null;
    }

    @Override
    public VersionSetRevision getVersionSetRevision(String versionSetName, String revisionId) throws VersionSetDoseNotExistsException {
        return null;
    }

    @Override
    public Path getBuildArtifact(ArchipelagoBuiltPackage pkg, Path directory) throws PackageNotFoundException {
        return null;
    }

    @Override
    public void createPackage(CreatePackageRequest request) throws PackageExistsException {

    }

    @Override
    public String getConfig(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException {
        return null;
    }

    @Override
    public GetPackageResponse getPackage(String pkg) throws PackageNotFoundException {
        return null;
    }
}
