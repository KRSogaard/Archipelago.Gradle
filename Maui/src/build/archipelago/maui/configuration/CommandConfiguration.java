package build.archipelago.maui.configuration;

import build.archipelago.maui.Output.OutputWrapper;
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
                                                     OutputWrapper outputWrapper,
                                                     WorkspaceSyncer workspaceSyncer) {
        return new WorkspaceSyncCommand(workspaceContextFactory, systemPathProvider, outputWrapper, workspaceSyncer);
    }

    @Provides
    @Singleton
    public WorkspaceCreateCommand workspaceCreateCommand(WorkspaceContextFactory workspaceContextFactory,
                                                         SystemPathProvider systemPathProvider,
                                                         OutputWrapper outputWrapper) {
        return new WorkspaceCreateCommand(workspaceContextFactory, systemPathProvider, outputWrapper);
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
                                                     PackageServiceClient packageServiceClient) {
        return new PackageCreateCommand(packageServiceClient, workspaceContextFactory, systemPathProvider, outputWrapper);
    }

    @Provides
    @Singleton
    public WorkspaceUseCommand workspaceUseCommand(VersionSetServiceClient versionSetServiceClient,
                                                   WorkspaceContextFactory workspaceContextFactory,
                                                   SystemPathProvider systemPathProvider,
                                                   OutputWrapper outputWrapper,
                                                   PackageServiceClient packageServiceClient,
                                                   PackageSourceProvider packageSourceProvider) {
        return new WorkspaceUseCommand(versionSetServiceClient, workspaceContextFactory, systemPathProvider, outputWrapper, packageServiceClient,
                packageSourceProvider);
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
