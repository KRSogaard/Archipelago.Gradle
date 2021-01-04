package build.archipelago.maui.commands;

import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.actions.BaseAction;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.path.MauiPath;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "path", mixinStandardHelpOptions = true, description = "Path")
public class PathCommand extends BaseAction implements Callable<Integer> {

    private MauiPath path;

    @CommandLine.Parameters(index = "0")
    private String pathLine;

    public PathCommand(MauiPath path,
                       WorkspaceContextFactory workspaceContextFactory,
                       SystemPathProvider systemPathProvider,
                       OutputWrapper out) {
        super(workspaceContextFactory, systemPathProvider, out);
        this.path = path;
    }

    @Override
    public Integer call() throws Exception {
        if (!setupWorkspaceContext()) {
            out.error("Was unable to locate the workspace");
            return 1;
        }
        if (!setupPackage()) {
            out.error("Was unable to locate the package");
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
        out.write(sb.toString());
        return 0;
    }
}
