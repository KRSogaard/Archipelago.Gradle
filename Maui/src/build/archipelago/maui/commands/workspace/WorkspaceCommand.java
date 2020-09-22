package build.archipelago.maui.commands.workspace;

import build.archipelago.maui.utils.SystemUtil;
import buils.archipelago.maui.serializer.WorkspaceConstants;
import picocli.CommandLine;

import java.nio.file.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "workspace", aliases = {"ws"}, mixinStandardHelpOptions = true, description = "Manipulation of the workspace",
subcommands = {WorkspaceSyncCommand.class, WorkspaceCreateCommand.class})
public class WorkspaceCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        return 1;
    }

    public Path getWorkspaceDir() {
        Path currentFolder = SystemUtil.getWorkingPath();

        while (currentFolder != null) {
            if (Files.exists(currentFolder.resolve(WorkspaceConstants.WORKSPACE_FILE_NAME))) {
                return currentFolder;
            }
            currentFolder = currentFolder.getParent();
        }
        return null;
    }
}
