package build.archipelago.maui.commands.workspace;

import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.maui.Output.OutputWrapper;
import build.archipelago.maui.commands.BaseCommand;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.core.workspace.contexts.*;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.*;

@Slf4j
@CommandLine.Command(name = "create", mixinStandardHelpOptions = true, description = "create a new workspace")
public class WorkspaceCreateCommand extends BaseCommand {

    @CommandLine.Option(names = { "-n", "--name"}, required = true)
    private String name;

    @CommandLine.Option(names = { "-vs", "--versionset"})
    private String versionSet;

    public WorkspaceCreateCommand(WorkspaceContextFactory workspaceContextFactory,
                                  SystemPathProvider systemPathProvider,
                                  OutputWrapper out) {
        super(workspaceContextFactory, systemPathProvider, out);
    }

    @Override
    public Integer call() throws Exception {
        Path dir = systemPathProvider.getCurrentDir();
        out.write("Creating workspace %s", name);
        Path wsRoot = dir.resolve(name);
        WorkspaceContext ws = workspaceContextFactory.create(wsRoot);

        if (Files.exists(wsRoot)) {
            out.error("The workspace \"%s\" already exists in this folder", name);
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
            out.error("Was unable to created the workspace as the requested version-set " +
                    "\"%s\" did not exist", versionSet);
            return 2;
        } catch (IOException e) {
            log.error("Failed to create the workspace file in \"" + wsRoot + "\"", e);
            out.error("Was unable to create the workspace file");
            return 1;
        }
        return 0;
    }
}