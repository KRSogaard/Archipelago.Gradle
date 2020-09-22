package build.archipelago.maui.configuration;

import build.archipelago.maui.commands.workspace.*;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import org.springframework.context.annotation.*;

@Configuration
public class WorkspaceCommandConfiguration {
    @Bean
    public WorkspaceCommand workspaceCommand() {
        return new WorkspaceCommand();
    }

    @Bean
    public WorkspaceSyncCommand workspaceSyncCommand() {
        return new WorkspaceSyncCommand();
    }

    @Bean
    public WorkspaceCreateCommand workspaceCreateCommand(VersionServiceClient versionServiceClient) {
        return new WorkspaceCreateCommand(versionServiceClient);
    }
}
