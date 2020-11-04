package build.archipelago.maui.path.recipies;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.common.contexts.WorkspaceContext;

import java.util.List;

public interface Recipe {
    String getName();
    List<String> execute(ArchipelagoPackage pkg, WorkspaceContext workspaceContext) throws Exception;
}
