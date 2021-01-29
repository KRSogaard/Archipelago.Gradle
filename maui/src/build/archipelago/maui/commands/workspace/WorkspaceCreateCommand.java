package build.archipelago.maui.commands.workspace;

import build.archipelago.maui.core.actions.WorkspaceCreateAction;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "create", mixinStandardHelpOptions = true, description = "create a new workspace")
public class WorkspaceCreateCommand implements Callable<Integer> {

    private WorkspaceCreateAction workspaceCreateAction;

    @CommandLine.Option(names = { "-n", "--name"}, required = true)
    private String name;

    @CommandLine.Option(names = { "-vs", "--versionset"})
    private String versionSet;

    public WorkspaceCreateCommand(WorkspaceCreateAction workspaceCreateAction) {
        this.workspaceCreateAction = workspaceCreateAction;
    }

    @Override
    public Integer call() throws Exception {
        if (workspaceCreateAction.createWorkspace(name, versionSet)) {
            return 0;
        }
        return 1;
    }
}