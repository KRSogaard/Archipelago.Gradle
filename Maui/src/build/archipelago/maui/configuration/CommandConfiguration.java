package build.archipelago.maui.configuration;

import build.archipelago.maui.commands.*;
import build.archipelago.maui.commands.packages.*;
import build.archipelago.maui.commands.workspace.*;
import build.archipelago.maui.core.workspace.*;
import build.archipelago.maui.core.workspace.cache.PackageCacher;
import build.archipelago.maui.core.workspace.path.MauiPath;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import com.google.inject.*;

public class CommandConfiguration extends AbstractModule {

    @Provides
    public WorkspaceCommand workspaceCommand() {
        return new WorkspaceCommand();
    }

    @Provides
    public WorkspaceSyncCommand workspaceSyncCommand(VersionServiceClient versionServiceClient,
                                                     PackageCacher packageCacher,
                                                     WorkspaceSyncer workspaceSyncer) {
        return new WorkspaceSyncCommand(versionServiceClient, workspaceSyncer, packageCacher);
    }

    @Provides
    public WorkspaceCreateCommand workspaceCreateCommand(VersionServiceClient versionServiceClient,
                                                         PackageCacher packageCacher) {
        return new WorkspaceCreateCommand(versionServiceClient, packageCacher);
    }

    @Provides
    public BuildCommand buildCommand(VersionServiceClient versionServiceClient,
                                     PackageCacher packageCacher,
                                     MauiPath mauiPath) {
        return new BuildCommand(versionServiceClient, packageCacher, mauiPath);
    }

    @Provides
    public PathCommand pathCommand(VersionServiceClient versionServiceClient,
                                   PackageCacher packageCacher,
                                   MauiPath mauiPath) {
        return new PathCommand(versionServiceClient, packageCacher, mauiPath);
    }

    @Provides
    public PackageCommand packageCommand() {
        return new PackageCommand();
    }

    @Provides
    public PackageCreateCommand packageCreateCommand(VersionServiceClient versionServiceClient,
                                                     PackageCacher packageCacher,
                                                     PackageServiceClient packageServiceClient) {
        return new PackageCreateCommand(versionServiceClient, packageCacher, packageServiceClient);
    }

    @Provides
    public WorkspaceUseCommand workspaceUseCommand(VersionServiceClient versionServiceClient,
                                                   PackageCacher packageCacher,
                                                   PackageServiceClient packageServiceClient,
                                                   PackageSourceProvider packageSourceProvider) {
        return new WorkspaceUseCommand(versionServiceClient, packageCacher,
                packageServiceClient, packageSourceProvider);
    }

    @Provides
    public WorkspaceRemoveCommand WorkspaceRemoveCommand(VersionServiceClient versionServiceClient,
                                                         PackageCacher packageCacher) {
        return new WorkspaceRemoveCommand(versionServiceClient, packageCacher);
    }

    @Provides
    public RecursiveCommand RecursiveCommand(VersionServiceClient versionServiceClient,
                                             PackageCacher packageCacher,
                                             MauiPath mauiPath) {
        return new RecursiveCommand(versionServiceClient, packageCacher, mauiPath);
    }

    @Provides
    public CleanCommand cleanCommand(VersionServiceClient versionServiceClient,
                                             PackageCacher packageCacher) {
        return new CleanCommand(versionServiceClient, packageCacher);
    }
}
