package build.archipelago.maui.configuration;

import build.archipelago.maui.commands.*;
import build.archipelago.maui.commands.packages.*;
import build.archipelago.maui.commands.workspace.*;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.actions.*;
import build.archipelago.maui.core.auth.AuthService;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.graph.DependencyGraphGenerator;
import build.archipelago.maui.path.MauiPath;
import com.google.inject.*;
import com.google.inject.name.Named;

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
    public WorkspaceSyncCommand workspaceSyncCommand(WorkspaceSyncAction workspaceSyncAction) {
        return new WorkspaceSyncCommand(workspaceSyncAction);
    }

    @Provides
    @Singleton
    public WorkspaceCreateCommand workspaceCreateCommand(WorkspaceCreateAction workspaceCreateAction) {
        return new WorkspaceCreateCommand(workspaceCreateAction);
    }

    @Provides
    @Singleton
    public BuildCommand buildCommand(BuildAction buildAction) {
        return new BuildCommand(buildAction);
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
    public PackageCreateCommand packageCreateCommand(PackageCreateAction packageCreateAction) {
        return new PackageCreateCommand(packageCreateAction);
    }

    @Provides
    @Singleton
    public WorkspaceUseCommand workspaceUseCommand(WorkspaceUseAction workspaceUseAction) {
        return new WorkspaceUseCommand(workspaceUseAction);
    }

    @Provides
    @Singleton
    public WorkspaceRemoveCommand WorkspaceRemoveCommand(WorkspaceRemoveAction workspaceRemoveAction) {
        return new WorkspaceRemoveCommand(workspaceRemoveAction);
    }

    @Provides
    @Singleton
    public RecursiveCommand RecursiveCommand(WorkspaceContextFactory workspaceContextFactory,
                                             SystemPathProvider systemPathProvider,
                                             OutputWrapper outputWrapper,
                                             DependencyGraphGenerator dependencyGraphGenerator) {
        return new RecursiveCommand(workspaceContextFactory, systemPathProvider, outputWrapper, dependencyGraphGenerator);
    }

    @Provides
    @Singleton
    public CleanCommand cleanCommand(CleanAction cleanAction) {
        return new CleanCommand(cleanAction);
    }

    @Provides
    @Singleton
    public VersionCommand versionCommand(OutputWrapper outputWrapper) {
        return new VersionCommand(outputWrapper);
    }

    @Provides
    @Singleton
    public AuthCommand authCommand(WorkspaceContextFactory workspaceContextFactory,
                                   SystemPathProvider systemPathProvider,
                                   OutputWrapper output,
                                   AuthService authService) {
        return new AuthCommand(workspaceContextFactory, systemPathProvider, output, authService);
    }
}
