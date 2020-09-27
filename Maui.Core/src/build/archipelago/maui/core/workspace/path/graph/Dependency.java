package build.archipelago.maui.core.workspace.path.graph;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.core.workspace.path.DependencyType;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Dependency {
    private DependencyType dependencyType;
    private ArchipelagoPackage pkg;

    public DependencyType getType() {
        return dependencyType;
    }

    public ArchipelagoPackage getPackage() {
        return pkg;
    }
    public void setPackage(ArchipelagoPackage pkg) {
        this.pkg = pkg;
    }
}
