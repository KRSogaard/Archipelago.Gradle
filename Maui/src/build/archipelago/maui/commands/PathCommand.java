package build.archipelago.maui.commands;

import build.archipelago.maui.core.workspace.cache.PackageCacher;
import build.archipelago.maui.core.workspace.path.MauiPath;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

@Slf4j
@CommandLine.Command(name = "path", mixinStandardHelpOptions = true, description = "Path")
public class PathCommand extends BaseCommand {

    private VersionServiceClient vsClient;
    private PackageCacher packageCacher;
    private MauiPath path;

    @CommandLine.Parameters(index = "0")
    private String pathLine;

    public PathCommand(VersionServiceClient vsClient, PackageCacher packageCacher, MauiPath path) {
        this.vsClient = vsClient;
        this.packageCacher = packageCacher;
        this.path = path;
    }

    @Override
    public Integer call() throws Exception {
        if (!requireWorkspace(vsClient, packageCacher)) {
            System.err.println("Was unable to locate the workspace");
            return 1;
        }
        if (!requirePackage()) {
            System.err.println("Was unable to locate the package");
            return 1;
        }

        ImmutableList<String> paths = path.getPaths(ws, pkg, pathLine);
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
