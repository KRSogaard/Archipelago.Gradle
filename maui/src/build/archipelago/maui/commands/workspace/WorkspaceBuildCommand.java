package build.archipelago.maui.commands.workspace;

import build.archipelago.maui.core.actions.WorkspaceCreateAction;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "build", mixinStandardHelpOptions = true, description = "Build the local workspace")
public class WorkspaceBuildCommand implements Callable<Integer> {

    @CommandLine.Option(names = { "-o", "--out"}, required = true)
    private String outDir;

    public WorkspaceBuildCommand(WorkspaceCreateAction workspaceCreateAction) {
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