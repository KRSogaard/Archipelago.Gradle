package build.archipelago.maui.core.workspace.path.recipies;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContext;

import java.util.List;

public interface Recipe {
    String getName();
    List<String> execute(ArchipelagoPackage pkg, WorkspaceContext workspaceContext) throws Exception;
}
