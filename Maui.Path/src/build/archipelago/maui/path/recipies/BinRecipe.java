package build.archipelago.maui.path.recipies;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.PackageNotBuiltException;
import build.archipelago.maui.common.contexts.WorkspaceContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class BinRecipe implements Recipe {
    @Override
    public String getName() {
        return "bin";
    }

    @Override
    public List<String> execute(ArchipelagoPackage pkg, WorkspaceContext workspaceContext)
            throws Exception {
        Path buildDir = workspaceContext.getPackageBuildPath(pkg);
        if (!Files.exists(buildDir) || !Files.isDirectory(buildDir)) {
            throw new PackageNotBuiltException(pkg);
        }
        Path binDir = buildDir.resolve("bin");
        if (!Files.exists(binDir)) {
            return List.of();
        }
        try {
            return List.of(binDir.toRealPath().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
