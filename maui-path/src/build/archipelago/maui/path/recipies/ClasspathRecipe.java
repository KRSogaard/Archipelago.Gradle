package build.archipelago.maui.path.recipies;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.PackageNotBuiltException;
import build.archipelago.maui.common.contexts.WorkspaceContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class ClasspathRecipe implements Recipe {
    @Override
    public String getName() {
        return "classpath";
    }

    @Override
    public List<String> execute(ArchipelagoPackage pkg, WorkspaceContext workspaceContext)
            throws Exception {
        log.debug("Running classpath recipe for package '{}'", pkg);
        Path buildDir = workspaceContext.getPackageBuildPath(pkg);
        if (!Files.exists(buildDir) || !Files.isDirectory(buildDir)) {
            throw new PackageNotBuiltException(pkg);
        }
        buildDir = buildDir.resolve("libs");
        if (!Files.exists(buildDir) || !Files.isDirectory(buildDir)) {
            log.debug("Package '{}' dose not have a libs folder", pkg.getNameVersion());
            return new ArrayList<>();
        }

        List<String> classPaths = new ArrayList<>();
        try {
            Files.walk(buildDir)
                    .filter(Files::isRegularFile)
                    .forEach(p -> {
                log.trace("Found file '{}'", p.toString());
                if ("jar".equalsIgnoreCase(FilenameUtils.getExtension(p.toString()))) {
                    try {
                        classPaths.add(p.toRealPath().toString());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return classPaths;
    }
}
