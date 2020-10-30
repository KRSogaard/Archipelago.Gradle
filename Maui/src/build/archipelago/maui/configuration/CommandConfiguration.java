package build.archipelago.maui.configuration;

import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.Output.OutputWrapper;
import build.archipelago.maui.commands.*;
import build.archipelago.maui.commands.packages.*;
import build.archipelago.maui.commands.workspace.*;
import build.archipelago.maui.common.PackageSourceProvider;
import build.archipelago.maui.common.cache.PackageCacher;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.path.MauiPath;
import com.google.inject.*;

import java.util.concurrent.ExecutorService;

public class CommandConfiguration extends AbstractModule {

    @Provides
    public MauiCommand mauiCommand() {
        return new MauiCommand();
    }

    @Provides
    @Singleton
    public WorkspaceCommand workspaceCommand() {
        return new WorkspaceCommand();
    }

    @Provides
    @Singleton
    public WorkspaceSyncCommand workspaceSyncCommand(WorkspaceContextFactory workspaceContextFactory,
                                                     SystemPathProvider systemPathProvider,
                                                     OutputWrapper out,
                                                     HarborClient harborClient,
                                                     PackageCacher packageCacher,
                                                     ExecutorService executorr) {
        return new WorkspaceSyncCommand(workspaceContextFactory, systemPathProvider, out, harborClient, packageCacher, executorr);
    }

    @Provides
    @Singleton
    public WorkspaceCreateCommand workspaceCreateCommand(WorkspaceContextFactory workspaceContextFactory,
                                                         SystemPathProvider systemPathProvider,
                                                         OutputWrapper out,
                                                         HarborClient harborClient) {
        return new WorkspaceCreateCommand(workspaceContextFactory, systemPathProvider, out, harborClient);
    }

    @Provides
    @Singleton
    public BuildCommand buildCommand(WorkspaceContextFactory workspaceContextFactory,
                                     SystemPathProvider systemPathProvider,
                                     OutputWrapper outputWrapper,
                                     MauiPath mauiPath) {
        return new BuildCommand(mauiPath, workspaceContextFactory, systemPathProvider, outputWrapper);
    }

    @Provides
    @Singleton
    public PathCommand pathCommand(WorkspaceContextFactory workspaceContextFactory,
                                   SystemPathProvider systemPathProvider,
                                   OutputWrapper outputWrapper,
                                   MauiPath mauiPath) {
        return new PathCommand(mauiPath, workspaceContextFactory, systemPathProvider, outputWrapper);
    }

    @Provides
    @Singleton
    public PackageCommand packageCommand() {
        return new PackageCommand();
    }

    @Provides
    @Singleton
    public PackageCreateCommand packageCreateCommand(WorkspaceContextFactory workspaceContextFactory,
                                                     SystemPathProvider systemPathProvider,
                                                     OutputWrapper outputWrapper,
                                                     HarborClient harborClient) {
        return new PackageCreateCommand(workspaceContextFactory, systemPathProvider, outputWrapper, harborClient);
    }

    @Provides
    @Singleton
    public WorkspaceUseCommand workspaceUseCommand(WorkspaceContextFactory workspaceContextFactory,
                                                   SystemPathProvider systemPathProvider,
                                                   OutputWrapper out,
                                                   HarborClient harborClient,
                                                   PackageSourceProvider packageSourceProvider) {
        return new WorkspaceUseCommand(workspaceContextFactory, systemPathProvider, out, harborClient, packageSourceProvider);
    }

    @Provides
    @Singleton
    public WorkspaceRemoveCommand WorkspaceRemoveCommand(WorkspaceContextFactory workspaceContextFactory,
                                                         SystemPathProvider systemPathProvider,
                                                         OutputWrapper outputWrapper) {
        return new WorkspaceRemoveCommand(workspaceContextFactory, systemPathProvider, outputWrapper);
    }

    @Provides
    @Singleton
    public RecursiveCommand RecursiveCommand(WorkspaceContextFactory workspaceContextFactory,
                                             SystemPathProvider systemPathProvider,
                                             OutputWrapper outputWrapper) {
        return new RecursiveCommand(workspaceContextFactory, systemPathProvider, outputWrapper);
    }

    @Provides
    @Singleton
    public CleanCommand cleanCommand(WorkspaceContextFactory workspaceContextFactory,
                                     SystemPathProvider systemPathProvider,
                                     OutputWrapper outputWrapper) {
        return new CleanCommand(workspaceContextFactory, systemPathProvider, outputWrapper);
    }

    @Provides
    @Singleton
    public VersionCommand versionCommand(OutputWrapper outputWrapper) {
        return new VersionCommand(outputWrapper);
    }
}
