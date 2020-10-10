package build.archipelago.maui.configuration;

import build.archipelago.maui.commands.*;
import build.archipelago.maui.commands.packages.*;
import build.archipelago.maui.commands.workspace.*;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.core.workspace.*;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.workspace.path.MauiPath;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import com.google.inject.*;

public class CommandConfiguration extends AbstractModule {

    @Provides
    @Singleton
    public WorkspaceCommand workspaceCommand() {
        return new WorkspaceCommand();
    }

    @Provides
    @Singleton
    public WorkspaceSyncCommand workspaceSyncCommand(WorkspaceContextFactory workspaceContextFactory,
                                                     SystemPathProvider systemPathProvider,
                                                     WorkspaceSyncer workspaceSyncer) {
        return new WorkspaceSyncCommand(workspaceContextFactory, systemPathProvider, workspaceSyncer);
    }

    @Provides
    @Singleton
    public WorkspaceCreateCommand workspaceCreateCommand(WorkspaceContextFactory workspaceContextFactory,
                                                         SystemPathProvider systemPathProvider) {
        return new WorkspaceCreateCommand(workspaceContextFactory, systemPathProvider);
    }

    @Provides
    @Singleton
    public BuildCommand buildCommand(WorkspaceContextFactory workspaceContextFactory,
                                     SystemPathProvider systemPathProvider,
                                     MauiPath mauiPath) {
        return new BuildCommand(mauiPath, workspaceContextFactory, systemPathProvider);
    }

    @Provides
    @Singleton
    public PathCommand pathCommand(WorkspaceContextFactory workspaceContextFactory,
                                   SystemPathProvider systemPathProvider,
                                   MauiPath mauiPath) {
        return new PathCommand(mauiPath, workspaceContextFactory, systemPathProvider);
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
                                                     PackageServiceClient packageServiceClient) {
        return new PackageCreateCommand(packageServiceClient, workspaceContextFactory, systemPathProvider);
    }

    @Provides
    @Singleton
    public WorkspaceUseCommand workspaceUseCommand(VersionSetServiceClient versionSetServiceClient,
                                                   WorkspaceContextFactory workspaceContextFactory,
                                                   SystemPathProvider systemPathProvider,
                                                   PackageServiceClient packageServiceClient,
                                                   PackageSourceProvider packageSourceProvider) {
        return new WorkspaceUseCommand(versionSetServiceClient, workspaceContextFactory, systemPathProvider, packageServiceClient,
                packageSourceProvider);
    }

    @Provides
    @Singleton
    public WorkspaceRemoveCommand WorkspaceRemoveCommand(WorkspaceContextFactory workspaceContextFactory,
                                                         SystemPathProvider systemPathProvider) {
        return new WorkspaceRemoveCommand(workspaceContextFactory, systemPathProvider);
    }

    @Provides
    @Singleton
    public RecursiveCommand RecursiveCommand(WorkspaceContextFactory workspaceContextFactory,
                                             SystemPathProvider systemPathProvider) {
        return new RecursiveCommand(workspaceContextFactory, systemPathProvider);
    }

    @Provides
    @Singleton
    public CleanCommand cleanCommand(WorkspaceContextFactory workspaceContextFactory,
                                     SystemPathProvider systemPathProvider) {
        return new CleanCommand(workspaceContextFactory, systemPathProvider);
    }
}
