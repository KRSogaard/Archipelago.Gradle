package build.archipelago.maui.configuration;

import build.archipelago.common.concurrent.BlockingExecutorServiceFactory;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.common.PackageSourceProvider;
import build.archipelago.maui.common.cache.PackageCacher;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.actions.*;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.path.MauiPath;
import com.google.inject.*;
import com.google.inject.name.Named;

import java.util.concurrent.ExecutorService;

public class ActionConfiguration extends AbstractModule {

    @Provides
    @Singleton
    public BuildAction buildAction(WorkspaceContextFactory workspaceContextFactory,
                                   SystemPathProvider systemPathProvider,
                                   OutputWrapper out,
                                   MauiPath mauiPath) {
        return new BuildAction(workspaceContextFactory, systemPathProvider, out, mauiPath);
    }

    @Provides
    @Singleton
    public CleanAction cleanAction(WorkspaceContextFactory workspaceContextFactory,
                                   SystemPathProvider systemPathProvider,
                                   OutputWrapper out) {
        return new CleanAction(workspaceContextFactory, systemPathProvider, out);
    }

    @Provides
    @Singleton
    public PackageCreateAction packageCreateAction(WorkspaceContextFactory workspaceContextFactory,
                                                   SystemPathProvider systemPathProvider,
                                                   OutputWrapper out,
                                                   HarborClient harborClient) {
        return new PackageCreateAction(workspaceContextFactory, systemPathProvider, out, harborClient);
    }

    @Provides
    @Singleton
    public WorkspaceCreateAction workspaceCreateAction(WorkspaceContextFactory workspaceContextFactory,
                                                       SystemPathProvider systemPathProvider,
                                                       OutputWrapper out,
                                                       HarborClient harborClient) {
        return new WorkspaceCreateAction(workspaceContextFactory, systemPathProvider, out, harborClient);
    }

    @Provides
    @Singleton
    public WorkspaceRemoveAction workspaceRemoveAction(WorkspaceContextFactory workspaceContextFactory,
                                                       SystemPathProvider systemPathProvider,
                                                       OutputWrapper out) {
        return new WorkspaceRemoveAction(workspaceContextFactory, systemPathProvider, out);
    }

    @Provides
    @Singleton
    public WorkspaceSyncAction workspaceSyncAction(WorkspaceContextFactory workspaceContextFactory,
                                                   SystemPathProvider systemPathProvider,
                                                   OutputWrapper out,
                                                   HarborClient harborClient,
                                                   PackageCacher packageCacher,
                                                   @Named("sync.threads") int syncThreads) {
        BlockingExecutorServiceFactory executorServiceFactory = new BlockingExecutorServiceFactory();
        executorServiceFactory.setMaximumPoolSize(syncThreads);
        executorServiceFactory.setQueueCapacity(1000000);

        return new WorkspaceSyncAction(workspaceContextFactory, systemPathProvider, out, harborClient, packageCacher, executorServiceFactory.create());
    }

    @Provides
    @Singleton
    public WorkspaceUseAction workspaceUseAction(WorkspaceContextFactory workspaceContextFactory,
                                                 SystemPathProvider systemPathProvider,
                                                 OutputWrapper out,
                                                 HarborClient harborClient,
                                                 PackageSourceProvider packageSourceProvider) {
        return new WorkspaceUseAction(workspaceContextFactory, systemPathProvider, out, harborClient, packageSourceProvider);

    }
}
