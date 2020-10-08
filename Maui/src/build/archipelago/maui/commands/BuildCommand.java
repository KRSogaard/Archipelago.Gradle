package build.archipelago.maui.commands;

import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.core.workspace.cache.PackageCacher;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.workspace.path.*;
import build.archipelago.maui.core.workspace.path.recipies.BinRecipe;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import picocli.CommandLine;
import java.nio.file.Files;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

@Slf4j
@CommandLine.Command(name = "build", mixinStandardHelpOptions = true, description = "Build a package")
public class BuildCommand extends BaseCommand {

    private static final String BASH_BUILD_SYSTEM = "bash";
    private static final String BASH_BUILD_FILE = "BuildScript";

    private MauiPath path;

    @CommandLine.Parameters(index = "0..*")
    private List<String> args;

    public BuildCommand(MauiPath path,
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

        List<Path> paths = path.getPaths(ws, pkg, DependencyTransversalType.BUILD_TOOLS, BinRecipe.class).stream()
                .map(sp -> Paths.get(sp))
                .collect(Collectors.toList());

        Path buildSystemFilePath = null;
        String buildSystem = buildConfig.getBuildSystem().trim().toLowerCase();
        if (BASH_BUILD_SYSTEM.equalsIgnoreCase(buildSystem)) {
            StringBuilder buildSystemFileNameBuilder = new StringBuilder();
            buildSystemFileNameBuilder.append(BASH_BUILD_FILE);
            if (SystemUtils.IS_OS_WINDOWS) {
                buildSystemFileNameBuilder.append(".cmd");
            }
            buildSystemFilePath = pkgDir.resolve(buildSystemFileNameBuilder.toString());
            if (!Files.exists(buildSystemFilePath)) {
                System.err.println(String.format("Was unable to find the build file \"%s\" in \"%s\"",
                        buildSystemFilePath.getFileName(), buildSystemFilePath.getParent().toString()));
                return 1;
            }
        } else {
            StringBuilder buildSystemFileNameBuilder = new StringBuilder();
            buildSystemFileNameBuilder.append(buildConfig.getBuildSystem().trim().toLowerCase());
            if (SystemUtils.IS_OS_WINDOWS) {
                buildSystemFileNameBuilder.append(".cmd");
            }
            final String buildSystemFileName = buildSystemFileNameBuilder.toString();
            for (Path binPath : paths) {
                Optional<Path> buildPath = Files.list(binPath).filter(binFile -> binFile.getFileName().toString()
                        .equalsIgnoreCase(buildSystemFileName)).findFirst();
                if (buildPath.isPresent()) {
                    buildSystemFilePath = buildPath.get();
                    break;
                }
            }
            if (buildSystemFilePath == null) {
                System.err.println(String.format("The build system %s was not found in the build dependency tree", buildConfig.getBuildSystem()));
            }
        }

        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(pkgDir.toFile());
        List<String> cmd = new ArrayList<>();
        cmd.add(buildSystemFilePath.toRealPath().toString());
        if (args != null) {
            cmd.addAll(args);
        }
        builder.command(cmd);

        Map<String, String> env = builder.environment();
        env.put("ARCHIPELAGO.PACKAGE", pkg.getNameVersion());
        env.put("ARCHIPELAGO.PACKAGE_NAME", pkg.getName());
        env.put("ARCHIPELAGO.PACKAGE_VERSION", pkg.getVersion());
        env.put("ARCHIPELAGO.PACKAGE_ROOT", pkgDir.toRealPath().toString());
        env.put("ARCHIPELAGO.WORKSPACE", wsDir.toRealPath().toString());

        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("The build failed.");
            return exitCode;
        }
        System.out.println("Build successful");
        return exitCode;
    }
}
