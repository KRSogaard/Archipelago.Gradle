package build.archipelago.buildserver.builder.clients;

import build.archipelago.harbor.client.HarborClient;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;

public class InternalHarborClientFactory {

    private VersionSetServiceClient versionSetServiceClient;
    private PackageServiceClient packageServiceClient;

    public InternalHarborClientFactory(
            VersionSetServiceClient versionSetServiceClient,
            PackageServiceClient packageServiceClient)
    {
        this.versionSetServiceClient = versionSetServiceClient;
        this.packageServiceClient = packageServiceClient;
    }

    public HarborClient create(String accountId) {
        return new InternalHarborClient(versionSetServiceClient, packageServiceClient, accountId);
    }
}
