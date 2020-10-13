package build.archipelago.maui.commands;

import build.archipelago.maui.Output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.core.workspace.WorkspaceConstants;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContextFactory;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

@Slf4j
@CommandLine.Command(name = "clean", mixinStandardHelpOptions = true, description = "Build a package")
public class CleanCommand extends BaseCommand {

    public CleanCommand(WorkspaceContextFactory workspaceContextFactory,
                        SystemPathProvider systemPathProvider,
                        OutputWrapper out) {
        super(workspaceContextFactory, systemPathProvider, out);
    }

    @Override
    public Integer call() throws Exception {
        if (!requireWorkspace()) {
            out.error("Was unable to locate the workspace");
            return 1;
        }
        if (!requirePackage()) {
            out.error("Was unable to locate the package");
            return 1;
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
        return 0;
    }
}
