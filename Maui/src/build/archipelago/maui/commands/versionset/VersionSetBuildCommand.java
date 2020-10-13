package build.archipelago.maui.commands.versionset;

import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.maui.Output.OutputWrapper;
import build.archipelago.maui.commands.BaseCommand;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContextFactory;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.nio.file.Path;

@Slf4j
@CommandLine.Command(name = "build", mixinStandardHelpOptions = true, description = "")
public class VersionSetBuildCommand extends BaseCommand {

    @CommandLine.Option(names = { "-vs", "--versionset"})
    private String versionSetName;

    @CommandLine.Option(names = { "-r", "--revision"})
    private String revisionId;

    private VersionSetServiceClient versionSetServiceClient;

    public VersionSetBuildCommand(WorkspaceContextFactory workspaceContextFactory,
                                  SystemPathProvider systemPathProvider,
                                  OutputWrapper out,
                                  VersionSetServiceClient versionSetServiceClient) {
        super(workspaceContextFactory, systemPathProvider, out);
        this.versionSetServiceClient = versionSetServiceClient;
    }

    @Override
    public Integer call() throws Exception {
        if (requireWorkspace()) {
            out.error("A Version-Set can not be build inside a workspace");
            return 1;
        }

        Path path = systemPathProvider.getCurrentDir().resolve("artifact");
        out.write("Build version-set artifact into \"%s\"", path.toString());

        VersionSet versionSet;
        try {
            versionSet = versionSetServiceClient.getVersionSet(versionSetName);
        } catch (VersionSetDoseNotExistsException e) {
            out.error("Unable to find the Version-Set \"%s\"", versionSetName);
            return 1;
        }

        return 0;
    }
}
