package build.archipelago.buildserver.builder.builder;

import build.archipelago.account.common.AccountService;
import build.archipelago.buildserver.builder.clients.InternalHarborClientFactory;
import build.archipelago.buildserver.builder.notifications.NotificationProvider;
import build.archipelago.buildserver.builder.output.S3OutputWrapperFactory;
import build.archipelago.buildserver.common.services.build.DynamoDBBuildService;
import build.archipelago.buildserver.common.services.build.logs.StageLogsService;
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
    private DynamoDBBuildService buildService;
    private AccountService accountService;
    private MauiPath mauiPath;
    private GitServiceFactory gitServiceFactory;
    private S3OutputWrapperFactory s3OutputWrapperFactory;
    private StageLogsService stageLogsService;
    private NotificationProvider notificationProvider;

    public BuilderFactory(InternalHarborClientFactory internalHarborClientFactory,
                          VersionSetServiceClient versionSetServiceClient,
                          PackageServiceClient packageServiceClient,
                          Path buildLocation,
                          GitServiceFactory gitServiceFactory,
                          S3OutputWrapperFactory s3OutputWrapperFactory,
                          StageLogsService stageLogsService,
                          DynamoDBBuildService buildService,
                          AccountService accountService,
                          MauiPath mauiPath,
                          NotificationProvider notificationProvider) {
        this.internalHarborClientFactory = internalHarborClientFactory;
        this.versionSetServiceClient = versionSetServiceClient;
        this.packageServiceClient = packageServiceClient;
        this.buildLocation = buildLocation;
        this.buildService = buildService;
        this.accountService = accountService;
        this.mauiPath = mauiPath;
        this.gitServiceFactory = gitServiceFactory;
        this.s3OutputWrapperFactory = s3OutputWrapperFactory;
        this.stageLogsService = stageLogsService;
        this.notificationProvider = notificationProvider;
    }

    public VersionSetBuilder create(BuildQueueMessage buildRequest) {
        return new VersionSetBuilder(internalHarborClientFactory, versionSetServiceClient,
                packageServiceClient, buildLocation, gitServiceFactory, s3OutputWrapperFactory, stageLogsService, buildService, accountService,
                mauiPath, buildRequest, notificationProvider);
    }
}
