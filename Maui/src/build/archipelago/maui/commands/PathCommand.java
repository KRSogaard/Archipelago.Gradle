package build.archipelago.maui.commands;

import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.workspace.path.MauiPath;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

@Slf4j
@CommandLine.Command(name = "path", mixinStandardHelpOptions = true, description = "Path")
public class PathCommand extends BaseCommand {

    private MauiPath path;

    @CommandLine.Parameters(index = "0")
    private String pathLine;

    public PathCommand(MauiPath path,
                       WorkspaceContextFactory workspaceContextFactory,
                       SystemPathProvider systemPathProvider) {
        super(workspaceContextFactory, systemPathProvider);
        this.path = path;
    }

    @Override
    public Integer call() throws Exception {
        if (!requireWorkspace()) {
            System.err.println("Was unable to locate the workspace");
            return 1;
        }
        if (!requirePackage()) {
            System.err.println("Was unable to locate the package");
            return 1;
        }

        ImmutableList<String> paths = path.getPaths(workspaceContext, commandPKG, pathLine);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String p : paths) {
            sb.append(p);
            if(first) {
                first = false;
            } else {
                sb.append(";");
            }
        }
        System.out.print(sb.toString());
        return 0;
    }
}
