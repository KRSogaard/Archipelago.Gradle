package build.archipelago.maui.configuration;

import build.archipelago.common.concurrent.*;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.core.workspace.*;
import build.archipelago.maui.core.workspace.cache.*;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.workspace.path.MauiPath;
import build.archipelago.maui.core.workspace.path.recipies.*;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.rest.RestPackageServiceClient;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import build.archipelago.versionsetservice.client.rest.RestVersionSetServiceClient;
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
    public VersionServiceClient versionServiceClient(@Named("services.versionset.url") String vsEndpoint) {
        return new RestVersionSetServiceClient(vsEndpoint);
    }

    @Provides
    @Singleton
    public PackageServiceClient packageServiceClient(@Named("services.packages.url") String pkgEndpoint) {
        return new RestPackageServiceClient(pkgEndpoint);
    }

    @Provides
    @Singleton
    public WorkspaceContextFactory workspaceContextFactory(VersionServiceClient versionServiceClient,
                                                           PackageCacher packageCacher) {
        return new WorkspaceContextFactory(versionServiceClient, packageCacher);
    }

    @Provides
    @Singleton
    public PackageCacher packageCacher(PackageServiceClient packageServiceClient,
                                       SystemPathProvider systemPathProvider) throws IOException {
        Path cachePath = systemPathProvider.getCachePath();
        if (!Files.exists(cachePath)) {
            Files.createDirectory(cachePath);
        }
        Path tempPath = systemPathProvider.getMauiPath().resolve("temp");
        if (!Files.exists(tempPath)) {
            Files.createDirectory(tempPath);
        }
        return new LocalPackageCacher(cachePath, tempPath, packageServiceClient);
    }

    @Provides
    @Singleton
    public WorkspaceSyncer workspaceSyncer(PackageCacher packageCacher,
                                           VersionServiceClient versionServiceClient) {
        BlockingExecutorServiceFactory executorServiceFactory = new BlockingExecutorServiceFactory();
        executorServiceFactory.setMaximumPoolSize(4);
        return new WorkspaceSyncer(packageCacher, versionServiceClient, executorServiceFactory);
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
    public PackageSourceProvider packageSourceProvider(VersionServiceClient versionServiceClient,
                                                       @Named("sourceprovider") String serviceProvider,
                                                       @Named("sourceprovider.git.base") String gitBase,
                                                       @Named("sourceprovider.git.group") String gitGroup) {
        return new GitPackageSourceProvider(versionServiceClient, gitBase, gitGroup);
    }
}
