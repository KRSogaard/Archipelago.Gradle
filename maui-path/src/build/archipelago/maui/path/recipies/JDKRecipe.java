package build.archipelago.maui.path.recipies;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.PackageNotBuiltException;
import build.archipelago.maui.common.contexts.WorkspaceContext;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class JDKRecipe implements Recipe {
    @Override
    public String getName() {
        return "jdk";
    }

    @Override
    public List<String> execute(ArchipelagoPackage pkg, WorkspaceContext workspaceContext) throws Exception {
        Path buildDir = workspaceContext.getPackageBuildPath(pkg);
        if (!Files.exists(buildDir) || !Files.isDirectory(buildDir)) {
            throw new PackageNotBuiltException(pkg);
        }

        List<String> results = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(buildDir, 1)) {
            List<String> pathResults = walk.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().toLowerCase().startsWith("jdk"))
                    .map(p -> p.toAbsolutePath().toString()).collect(Collectors.toList());
            results.addAll(pathResults);

        } catch (IOException e) {
            log.warn("Failed to walk '" + buildDir + "'", e);
        }

        return results;
    }
}
