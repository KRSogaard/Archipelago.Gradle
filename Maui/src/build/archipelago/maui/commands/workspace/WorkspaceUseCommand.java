package build.archipelago.maui.commands.workspace;

import build.archipelago.maui.core.actions.WorkspaceUseAction;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "use", mixinStandardHelpOptions = true, description = "Checkout packages or version-set")
public class WorkspaceUseCommand implements Callable<Integer> {

    @CommandLine.Option(names = { "-p", "--package"})
    private List<String> packages;

    @CommandLine.Option(names = { "-vs", "--versionset"})
    private String versionSet;

    private WorkspaceUseAction workspaceUseAction;

    public WorkspaceUseCommand(
            WorkspaceUseAction workspaceUseAction) {
        this.workspaceUseAction = workspaceUseAction;
    }

    @Override
    public Integer call() throws Exception {

        if (versionSet == null && packages == null) {
            System.err.println("No package or version-set was provided");
            return 1;
        }

        if (versionSet != null) {
            if (!workspaceUseAction.useVersionSet(versionSet)) {
                return 1;
            }
        }

        if (packages != null) {
            if (!workspaceUseAction.usePackages(packages)) {
                return 1;
            }
        }

        return 0;
    }
}
