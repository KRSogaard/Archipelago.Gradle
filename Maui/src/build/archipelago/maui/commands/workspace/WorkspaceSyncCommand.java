package build.archipelago.maui.commands.workspace;

import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContext;
import build.archipelago.maui.core.workspace.WorkspaceSyncer;
import build.archipelago.maui.utils.WorkspaceUtils;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.FileNotFoundException;
import java.nio.file.*;
import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "sync", mixinStandardHelpOptions = true, description = "Synchronize the current workspace the the version-set")
public class WorkspaceSyncCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private WorkspaceCommand parent;

    @CommandLine.Option(names = { "-rev", "--revision"})
    private String revision;

    private VersionServiceClient vsClient;
    private WorkspaceSyncer workspaceSyncer;

    public WorkspaceSyncCommand(VersionServiceClient vsClient, WorkspaceSyncer workspaceSyncer) {
        this.vsClient = vsClient;
        this.workspaceSyncer = workspaceSyncer;
    }

    @Override
    public Integer call() throws Exception {
        Path wsDir = WorkspaceUtils.getWorkspaceDir();
        if (wsDir == null) {
            System.err.printf("Was unable to locate the workspace");
            return 1;
        }

        WorkspaceContext ws = new WorkspaceContext(wsDir, vsClient);
        try {
            ws.load();
        } catch (FileNotFoundException exp) {
            log.error("Was unable to find the workspace file after we had identified the workspace " +
                    "directory to \"{}\", was it removed since we found the dir?", wsDir);
            System.err.printf("Was unable to locate the workspace");
            return 1;
        }

        if (revision != null) {
            revision = revision.trim();
        }
        try {
            VersionSetRevision vsRevision = workspaceSyncer.syncVersionSet(ws.getVersionSet(), revision);
            ws.saveRevisionCache(vsRevision);

        } catch (VersionSetDoseNotExistsException e) {
            log.error(String.format("Was unable to sync workspace \"%s\" as the version set \"%s#%s\" dose not exists",
                    wsDir.toString(), ws.getVersionSet(), revision), e);
            if (revision != null) {
                System.err.printf("Was unable to sync the workspace as the version set \"%s#%s\" dose not exists",
                        ws.getVersionSet(), revision);
            } else {
                System.err.printf("Was unable to sync the workspace as the version set \"%s\" dose not exists",
                        ws.getVersionSet());
            }
        }

        return 0;
    }
}
