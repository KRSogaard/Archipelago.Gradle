package build.archipelago.maui.core.actions;

import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.graph.DependencyTransversalType;
import build.archipelago.maui.path.MauiPath;
import build.archipelago.maui.path.recipies.BinRecipe;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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
        List<String> fileExtOptions = new ArrayList<>();
        if (SystemUtils.IS_OS_WINDOWS) {
            fileExtOptions.add(".cmd");
            fileExtOptions.add(".bat");
        } else {
            fileExtOptions.add("");
            fileExtOptions.add(".sh");
        }

        log.debug("Build system is '{}'", buildSystem);
        if (BASH_BUILD_SYSTEM.equalsIgnoreCase(buildSystem)) {
            StringBuilder buildSystemFileNameBuilder = new StringBuilder();
            buildSystemFileNameBuilder.append(BASH_BUILD_FILE);
            buildSystemFilePath = getBuildSystemInDir(BASH_BUILD_FILE, fileExtOptions, pkgDir);

            if (buildSystemFilePath == null) {
                out.error("Was unable to find the build file \"%s\" in \"%s\"",
                        BASH_BUILD_FILE, buildSystemFilePath.getParent().toString());
                return false;
            }
        } else {
            StringBuilder buildSystemFileNameBuilder = new StringBuilder();
            buildSystemFileNameBuilder.append(buildConfig.getBuildSystem().trim().toLowerCase());
            log.debug("Looking for file '{}'", buildConfig.getBuildSystem());
            for (Path binPath : paths) {
                buildSystemFilePath = getBuildSystemInDir(buildSystem, fileExtOptions, binPath);
                if (buildSystemFilePath != null) {
                    break;
                }
            }

            if (buildSystemFilePath == null) {
                out.error("The build system %s was not found in the build dependency tree", buildConfig.getBuildSystem());
                return false;
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
            out.error("The build failed with non 0 error code.");
            return false;
        }
        out.write("Build successful");
        return true;
    }

    private Path getBuildSystemInDir(String buildSystem, List<String> exts, Path dir) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildSystem), "Provided build system was null or empty");
        String bs = buildSystem.trim();
        List<Path> paths = getFilesInDir(dir);
        if (paths == null || paths.size() == 0) {
            return null;
        }

        for (Path p : paths) {
            String pathFileName = p.getFileName().toString();
            for (String ext : exts) {
                String test = bs + ext;
                if (pathFileName.equalsIgnoreCase(test)) {
                    return p;
                }
            }
        }
        return null;
    }

    private List<Path> getFilesInDir(Path dir) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            log.warn("The path '{}' did not exists or was file", dir);
            return new ArrayList<>();
        }
        try {
            return Files.list(dir)
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
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
