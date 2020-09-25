package build.archipelago.maui.configuration;

import build.archipelago.maui.commands.workspace.*;
import build.archipelago.maui.core.workspace.WorkspaceSyncer;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import org.springframework.context.annotation.*;

@Configuration
public class WorkspaceCommandConfiguration {
    @Bean
    public WorkspaceCommand workspaceCommand() {
        return new WorkspaceCommand();
    }

    @Bean
    public WorkspaceSyncCommand workspaceSyncCommand(VersionServiceClient versionServiceClient,
                                                     WorkspaceSyncer workspaceSyncer) {
        return new WorkspaceSyncCommand(versionServiceClient, workspaceSyncer);
    }

    @Bean
    public WorkspaceCreateCommand workspaceCreateCommand(VersionServiceClient versionServiceClient) {
        return new WorkspaceCreateCommand(versionServiceClient);
    }
}
