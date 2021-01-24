package build.archipelago.maui.core.actions;

import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.graph.DependencyTransversalType;
import build.archipelago.maui.path.MauiPath;
import build.archipelago.maui.path.recipies.BinRecipe;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class BuildAction extends BaseAction {

    private static final String BASH_BUILD_SYSTEM = "bash";
    private static final String BASH_BUILD_FILE = "BuildScript";

    private MauiPath path;

    public BuildAction(WorkspaceContextFactory workspaceContextFactory, SystemPathProvider systemPathProvider, OutputWrapper out, MauiPath path) {
        super(workspaceContextFactory, systemPathProvider, out);
        this.path = path;
    }

    public boolean build(List<String> args) throws Exception {
        if (!this.setupWorkspaceContext()) {
            out.error("Was unable to locate the workspace");
            return false;
        }
        if (!this.setupPackage()) {
            out.error("Was unable to locate the package");
            return false;
        }

        List<Path> paths = path.getPaths(workspaceContext, commandPKG, DependencyTransversalType.BUILD, BinRecipe.class).stream()
                .map(Paths::get)
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
                out.error("Was unable to find the build file \"%s\" in \"%s\"",
                        buildSystemFilePath.getFileName(), buildSystemFilePath.getParent().toString());
                return false;
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
                out.error("The build system %s was not found in the build dependency tree", buildConfig.getBuildSystem());
            }
        }

        ProcessBuilder builder = new ProcessBuilder();
        builder.redirectErrorStream(true);
        builder.directory(pkgDir.toFile());
        List<String> cmd = new ArrayList<>();
        cmd.add(buildSystemFilePath.toRealPath().toString());
        if (args != null) {
            cmd.addAll(args);
        }
        builder.command(cmd);

        Map<String, String> env = builder.environment();
        env.put("ARCHIPELAGO.PACKAGE", commandPKG.getNameVersion());
        env.put("ARCHIPELAGO.PACKAGE_NAME", commandPKG.getName());
        env.put("ARCHIPELAGO.PACKAGE_VERSION", commandPKG.getVersion());
        env.put("ARCHIPELAGO.PACKAGE_ROOT", pkgDir.toRealPath().toString());
        env.put("ARCHIPELAGO.WORKSPACE", wsDir.toRealPath().toString());

        Process process = builder.start();
        CompletableFuture<Void> errorReader = redirectOut(process.getErrorStream(), line -> out.error(line));
        CompletableFuture<Void> outReader = redirectOut(process.getInputStream(), line -> out.write(line));

        boolean wasTerminated = !process.waitFor(15, TimeUnit.MINUTES);
        process.destroy();
        if (process.isAlive()) {
            process.waitFor(10, TimeUnit.SECONDS); // give it a chance to stop
            process.destroyForcibly();
        }
        if (wasTerminated) {
            out.error("The build failed to finish within the allowed 15 min, was terminated");
            out.error("The build failed.");
            return false;
        }
        int exitCode = process.waitFor();
        errorReader.cancel(true);
        outReader.cancel(true);
        if (exitCode != 0) {
            out.error("The build failed.");
            return false;
        }
        out.write("Build successful");
        return true;
    }

    private static CompletableFuture<Void> redirectOut(InputStream in, Consumer<String> out) {
        return CompletableFuture.runAsync(() -> {
            try (
                    InputStreamReader inputStreamReader = new InputStreamReader(in);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader)
            ) {
                bufferedReader.lines()
                        .forEach(out);
            } catch (IOException e) {
                log.error("Failed to redirect process output", e);
            }
        });
    }
}
