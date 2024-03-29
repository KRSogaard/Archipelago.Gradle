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
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class DirRecipe implements Recipe {
    @Override
    public String getName() {
        return "dir";
    }

    @Override
    public List<String> execute(ArchipelagoPackage pkg, WorkspaceContext workspaceContext) throws Exception {
        Path buildDir = workspaceContext.getPackageBuildPath(pkg);
        if (!Files.exists(buildDir) || !Files.isDirectory(buildDir)) {
            throw new PackageNotBuiltException(pkg);
        }
        return List.of(buildDir.toRealPath().toString());
    }
}
