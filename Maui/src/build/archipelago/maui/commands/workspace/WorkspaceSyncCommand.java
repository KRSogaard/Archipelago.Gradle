package build.archipelago.maui.commands.workspace;

import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.maui.commands.BaseCommand;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.core.workspace.WorkspaceSyncer;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContextFactory;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

@Slf4j
@CommandLine.Command(name = "sync", mixinStandardHelpOptions = true, description = "Synchronize the current workspace the the version-set")
public class WorkspaceSyncCommand extends BaseCommand {

    @CommandLine.ParentCommand
    private WorkspaceCommand parent;

    @CommandLine.Option(names = { "-rev", "--revision"})
    private String revision;

    private WorkspaceSyncer workspaceSyncer;

    public WorkspaceSyncCommand(WorkspaceContextFactory workspaceContextFactory,
                                SystemPathProvider systemPathProvider,
                                WorkspaceSyncer workspaceSyncer) {
        super(workspaceContextFactory, systemPathProvider);
        this.workspaceSyncer = workspaceSyncer;
    }

    @Override
    public Integer call() throws Exception {
        if (!requireWorkspace()) {
            System.err.println("Was unable to locate the workspace");
            return 1;
        }

        if (workspaceContext.getVersionSet() == null) {
            System.out.println("No version set is assigned to the workspace, can not sync.");
            return 1;
        }

        if (revision != null) {
            revision = revision.trim();
        }
        try {
            VersionSetRevision vsRevision = workspaceSyncer.syncVersionSet(workspaceContext.getVersionSet(), revision);
            workspaceContext.saveRevisionCache(vsRevision);

        } catch (VersionSetDoseNotExistsException e) {
            log.error(String.format("Was unable to sync workspace \"%s\" as the version set \"%s#%s\" dose not exists",
                    wsDir.toString(), workspaceContext.getVersionSet(), revision), e);
            if (revision != null) {
                System.err.println(String.format("Was unable to sync the workspace as the version set \"%s#%s\" dose not exists",
                        workspaceContext.getVersionSet(), revision));
            } else {
                System.err.println(String.format("Was unable to sync the workspace as the version set \"%s\" dose not exists",
                        workspaceContext.getVersionSet()));
            }
        }

        return 0;
    }
}
