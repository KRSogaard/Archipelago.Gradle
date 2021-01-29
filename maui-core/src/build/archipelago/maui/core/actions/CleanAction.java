package build.archipelago.maui.core.actions;

import build.archipelago.maui.common.WorkspaceConstants;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;

import java.io.File;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

public class CleanAction extends BaseAction {
    public CleanAction(WorkspaceContextFactory workspaceContextFactory, SystemPathProvider systemPathProvider, OutputWrapper out) {
        super(workspaceContextFactory, systemPathProvider, out);
    }

    public boolean clean() throws Exception {
        if (!setupWorkspaceContext()) {
            out.error("Was unable to locate the workspace");
            return false;
        }
        if (!setupPackage()) {
            out.error("Was unable to locate the package");
            return false;
        }

        Path buildDir = pkgDir.resolve(WorkspaceConstants.BUILD_DIR);
        if (Files.exists(buildDir) && Files.isDirectory(buildDir)) {
            try (Stream<Path> walk = Files.walk(buildDir)) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }

        out.write("Clean successful");
        return true;
    }
}
