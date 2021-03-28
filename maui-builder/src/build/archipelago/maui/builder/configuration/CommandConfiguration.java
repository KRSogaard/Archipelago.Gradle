package build.archipelago.maui.builder.configuration;

import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.builder.commands.MauiCommand;
import build.archipelago.maui.builder.commands.PathCommand;
import build.archipelago.maui.builder.commands.VersionCommand;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.actions.*;
import build.archipelago.maui.core.auth.AuthService;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.graph.DependencyGraphGenerator;
import build.archipelago.maui.path.MauiPath;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class CommandConfiguration extends AbstractModule {

    @Provides
    public MauiCommand mauiCommand() {
        return new MauiCommand();
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
    public VersionCommand versionCommand(OutputWrapper outputWrapper) {
        return new VersionCommand(outputWrapper);
    }
}
