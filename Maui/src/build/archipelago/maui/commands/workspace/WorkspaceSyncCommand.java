package build.archipelago.maui.commands.workspace;

import picocli.CommandLine;
import java.nio.file.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "sync", mixinStandardHelpOptions = true, description = "Synchronize the current workspace the the version-set")
public class WorkspaceSyncCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private WorkspaceCommand parent;

    public WorkspaceSyncCommand() {
    }

    @Override
    public Integer call() throws Exception {
        Path wsDir = parent.getWorkspaceDir();
        if (wsDir == null) {
            System.err.printf("Was unable to locate the workspace");
            return 1;
        }
        System.out.printf("Workspace found at %s", wsDir);
        return 0;
    }
}
