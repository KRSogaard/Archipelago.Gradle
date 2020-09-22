package build.archipelago.maui.commands.workspace;

import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.maui.common.workspace.Workspace;
import build.archipelago.maui.core.context.WorkspaceContext;
import build.archipelago.maui.utils.SystemUtil;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "create", mixinStandardHelpOptions = true, description = "create a new workspace")
public class WorkspaceCreateCommand implements Callable<Integer> {

    @CommandLine.Option(names = { "-n", "--name"}, required = true)
    private String name;

    @CommandLine.Option(names = { "-vs", "--versionset"})
    private String versionSet;

    private VersionServiceClient vsClient;

    public WorkspaceCreateCommand(VersionServiceClient vsClient) {
        this.vsClient = vsClient;
    }

    @Override
    public Integer call() throws Exception {
        Path dir = SystemUtil.getWorkingPath();
        System.out.printf("Creating workspace %s", name);
        Path wsRoot = dir.resolve(name);
        WorkspaceContext ws = new WorkspaceContext(wsRoot, vsClient);

        if (Files.exists(wsRoot)) {
            System.err.printf("The workspace \"%s\" already exists in this folder", name);
            return 1;
        }
        Files.createDirectory(wsRoot);

        if (!Strings.isNullOrEmpty(versionSet)) {
            ws.setVersionSet(versionSet);
        }

        try {
            ws.create();
        } catch (VersionSetDoseNotExistsException e) {
            log.error("Was unable to created the workspace as the requested version-set \"{}\" did not exist",
                    versionSet);
            System.err.printf("Was unable to created the workspace as the requested version-set " +
                    "\"%s\" did not exist", versionSet);
            return 2;
        } catch (IOException e) {
            log.error("Failed to create the workspace file in \"" + wsRoot + "\"", e);
            System.err.printf("Was unable to create the workspace file");
            return 1;
        }
        return 0;
    }
}