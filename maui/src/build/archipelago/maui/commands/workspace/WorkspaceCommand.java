package build.archipelago.maui.commands.workspace;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "workspace", aliases = {"ws"}, mixinStandardHelpOptions = true, description = "Manipulation of the workspace",
subcommands = {WorkspaceSyncCommand.class, WorkspaceCreateCommand.class, WorkspaceUseCommand.class, WorkspaceRemoveCommand.class})
public class WorkspaceCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        return 1;
    }
}
