package build.archipelago.buildserver.builder.builder;

import build.archipelago.account.common.AccountService;
import build.archipelago.buildserver.builder.clients.InternalHarborClientFactory;
import build.archipelago.buildserver.builder.output.S3OutputWrapperFactory;
import build.archipelago.buildserver.common.services.build.BuildService;
import build.archipelago.buildserver.common.services.build.models.BuildQueueMessage;
import build.archipelago.common.github.GitServiceFactory;
import build.archipelago.maui.path.MauiPath;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;

import java.nio.file.Path;

public class BuilderFactory {

    private InternalHarborClientFactory internalHarborClientFactory;
    private VersionSetServiceClient versionSetServiceClient;
    private PackageServiceClient packageServiceClient;
    private Path buildLocation;
    private BuildService buildService;
    private AccountService accountService;
    private MauiPath mauiPath;
    private GitServiceFactory gitServiceFactory;
    private S3OutputWrapperFactory s3OutputWrapperFactory;

    public BuilderFactory(InternalHarborClientFactory internalHarborClientFactory,
                          VersionSetServiceClient versionSetServiceClient,
                          PackageServiceClient packageServiceClient,
                          Path buildLocation,
                          GitServiceFactory gitServiceFactory,
                          S3OutputWrapperFactory s3OutputWrapperFactory,
                          BuildService buildService,
                          AccountService accountService,
                          MauiPath mauiPath) {
        this.internalHarborClientFactory = internalHarborClientFactory;
        this.versionSetServiceClient = versionSetServiceClient;
        this.packageServiceClient = packageServiceClient;
        this.buildLocation = buildLocation;
        this.buildService = buildService;
        this.accountService = accountService;
        this.mauiPath = mauiPath;
        this.gitServiceFactory = gitServiceFactory;
        this.s3OutputWrapperFactory = s3OutputWrapperFactory;
    }

    public VersionSetBuilder create(BuildQueueMessage buildRequest) {
        return new VersionSetBuilder(internalHarborClientFactory, versionSetServiceClient,
                packageServiceClient, buildLocation, gitServiceFactory, s3OutputWrapperFactory, buildService, accountService,
                mauiPath, buildRequest);
    }
}
