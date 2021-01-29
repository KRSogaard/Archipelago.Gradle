package build.archipelago.maui.commands.workspace;

import build.archipelago.maui.core.actions.WorkspaceSyncAction;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "sync", mixinStandardHelpOptions = true, description = "Synchronize the current workspace the the version-set")
public class WorkspaceSyncCommand implements Callable<Integer> {

    @CommandLine.Option(names = { "-rev", "--revision"})
    private String revision;

    private WorkspaceSyncAction workspaceSyncAction;

    public WorkspaceSyncCommand(WorkspaceSyncAction workspaceSyncAction) {
        this.workspaceSyncAction = workspaceSyncAction;
    }

    @Override
    public Integer call() throws Exception {
        if (workspaceSyncAction.syncWorkspace(revision)) {
            return 0;
        }
        return 1;
    }
}
