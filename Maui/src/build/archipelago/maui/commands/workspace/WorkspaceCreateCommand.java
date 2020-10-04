package build.archipelago.maui.commands.workspace;

import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.maui.commands.BaseCommand;
import build.archipelago.maui.core.workspace.cache.PackageCacher;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContext;
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
public class WorkspaceCreateCommand extends BaseCommand {

    @CommandLine.Option(names = { "-n", "--name"}, required = true)
    private String name;

    @CommandLine.Option(names = { "-vs", "--versionset"})
    private String versionSet;

    private VersionServiceClient vsClient;
    private PackageCacher packageCacher;

    public WorkspaceCreateCommand(VersionServiceClient vsClient, PackageCacher packageCacher) {
        this.vsClient = vsClient;
        this.packageCacher = packageCacher;
    }

    @Override
    public Integer call() throws Exception {
        Path dir = SystemUtil.getWorkingPath();
        System.out.println(String.format("Creating workspace %s", name));
        Path wsRoot = dir.resolve(name);
        WorkspaceContext ws = new WorkspaceContext(wsRoot, vsClient, packageCacher);

        if (Files.exists(wsRoot)) {
            System.err.println(String.format("The workspace \"%s\" already exists in this folder", name));
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
            System.err.println(String.format("Was unable to created the workspace as the requested version-set " +
                    "\"%s\" did not exist", versionSet));
            return 2;
        } catch (IOException e) {
            log.error("Failed to create the workspace file in \"" + wsRoot + "\"", e);
            System.err.println("Was unable to create the workspace file");
            return 1;
        }
        return 0;
    }
}