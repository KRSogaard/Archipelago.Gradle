package build.archipelago.maui.graph;

import build.archipelago.common.ArchipelagoPackage;
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
