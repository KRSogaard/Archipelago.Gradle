package build.archipelago.maui.core.workspace.path.recipies;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.core.exceptions.*;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContext;

import java.io.*;
import java.nio.file.*;
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
