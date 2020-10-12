package build.archipelago.maui.configuration;

import build.archipelago.common.concurrent.BlockingExecutorServiceFactory;
import build.archipelago.maui.MauiConstants;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.core.workspace.*;
import build.archipelago.maui.core.workspace.cache.*;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.workspace.path.MauiPath;
import build.archipelago.maui.core.workspace.path.recipies.*;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.rest.RestPackageServiceClient;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import build.archipelago.versionsetservice.client.rest.RestVersionSetSetServiceClient;
import com.google.inject.*;
import com.google.inject.name.Named;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class ServiceConfiguration extends AbstractModule {

    @Provides
    @Singleton
    public SystemPathProvider systemPathProvider() {
        return new SystemPathProvider();
    }

    @Provides
    @Singleton
    public VersionSetServiceClient versionServiceClient(@Named("services.versionset.url") String vsEndpoint) {
        return new RestVersionSetSetServiceClient(vsEndpoint);
    }

    @Provides
    @Singleton
    public PackageServiceClient packageServiceClient(@Named("services.packages.url") String pkgEndpoint) {
        return new RestPackageServiceClient(pkgEndpoint);
    }

    @Provides
    @Singleton
    public WorkspaceContextFactory workspaceContextFactory(VersionSetServiceClient versionSetServiceClient,
                                                           PackageCacher packageCacher) {
        return new WorkspaceContextFactory(versionSetServiceClient, packageCacher);
    }

    @Provides
    @Singleton
    public PackageCacher packageCacher(PackageServiceClient packageServiceClient,
                                       SystemPathProvider systemPathProvider) throws IOException {
        Path basePath = systemPathProvider.getMauiPath();
        if ("true".equalsIgnoreCase(System.getProperty(MauiConstants.ENV_USE_LOCAL_WORKSPACE_CACHE, "false"))) {
            basePath = systemPathProvider.getWorkspaceDir().resolve(".archipelago");
        }
        Path cachePath = basePath.resolve("cache");
        if (!Files.exists(cachePath)) {
            Files.createDirectory(cachePath);
        }
        Path tempPath = basePath.resolve("temp");
        if (!Files.exists(tempPath)) {
            Files.createDirectory(tempPath);
        }
        return new LocalPackageCacher(cachePath, tempPath, packageServiceClient);
    }

    @Provides
    @Singleton
    public WorkspaceSyncer workspaceSyncer(PackageCacher packageCacher,
                                           VersionSetServiceClient versionSetServiceClient) {
        BlockingExecutorServiceFactory executorServiceFactory = new BlockingExecutorServiceFactory();
        executorServiceFactory.setMaximumPoolSize(4);
        return new WorkspaceSyncer(packageCacher, versionSetServiceClient, executorServiceFactory);
    }

    @Provides
    @Singleton
    public MauiPath mauiPath() {
        return new MauiPath(List.of(
                new BinRecipe(),
                new PackageRecipe()
        ));
    }

    @Provides
    @Singleton
    public PackageSourceProvider packageSourceProvider(VersionSetServiceClient versionSetServiceClient,
                                                       @Named("sourceprovider") String serviceProvider,
                                                       @Named("sourceprovider.git.base") String gitBase,
                                                       @Named("sourceprovider.git.group") String gitGroup) {
        return new GitPackageSourceProvider(versionSetServiceClient, gitBase, gitGroup);
    }
}
