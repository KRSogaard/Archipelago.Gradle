package build.archipelago.maui.path.recipies;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.common.contexts.WorkspaceContext;

import java.util.List;

public class PackageRecipe implements Recipe {
    @Override
    public String getName() {
        return "package";
    }

    @Override
    public List<String> execute(ArchipelagoPackage pkg, WorkspaceContext workspaceContext) throws Exception {
        return List.of(pkg.getNameVersion());
    }
}
