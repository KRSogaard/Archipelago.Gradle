package build.archipelago.maui.commands.versionset;

import build.archipelago.common.versionset.VersionSet;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.actions.BaseAction;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "build", mixinStandardHelpOptions = true, description = "")
public class VersionSetBuildCommand extends BaseAction implements Callable<Integer> {

    @CommandLine.Option(names = { "-vs", "--versionset"})
    private String versionSetName;

    @CommandLine.Option(names = { "-r", "--revision"})
    private String revisionId;

    private HarborClient harborClient;

    public VersionSetBuildCommand(WorkspaceContextFactory workspaceContextFactory,
                                  SystemPathProvider systemPathProvider,
                                  OutputWrapper out,
                                  HarborClient harborClient) {
        super(workspaceContextFactory, systemPathProvider, out);
        this.harborClient = harborClient;
    }

    @Override
    public Integer call() throws Exception {
        if (setupWorkspaceContext()) {
            out.error("A Version-Set can not be build inside a workspace");
            return 1;
        }

        Path path = systemPathProvider.getCurrentDir().resolve("artifact");
        out.write("Build version-set artifact into \"%s\"", path.toString());

        VersionSet versionSet;
        try {
            versionSet = harborClient.getVersionSet(versionSetName);
        } catch (VersionSetDoseNotExistsException e) {
            out.error("Unable to find the Version-Set \"%s\"", versionSetName);
            return 1;
        }

        return 0;
    }
}
