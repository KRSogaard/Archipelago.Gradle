package build.archipelago.maui.commands.workspace;

import build.archipelago.maui.core.actions.WorkspaceRemoveAction;
import picocli.CommandLine;

import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "remove", aliases = {"rm"}, mixinStandardHelpOptions = true, description = "Checkout packages or version-set")
public class WorkspaceRemoveCommand implements Callable<Integer> {

    private WorkspaceRemoveAction workspaceRemoveAction;

    @CommandLine.Option(names = { "-p", "--package"})
    private List<String> packages;

    public WorkspaceRemoveCommand(WorkspaceRemoveAction workspaceRemoveAction) {
        this.workspaceRemoveAction = workspaceRemoveAction;
    }

    @Override
    public Integer call() throws Exception {
        if (workspaceRemoveAction.removePackages(packages)) {
            return 0;
        }
        return 1;
    }
}
