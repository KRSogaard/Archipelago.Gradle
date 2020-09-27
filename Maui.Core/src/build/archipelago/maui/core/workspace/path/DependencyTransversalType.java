package build.archipelago.maui.core.workspace.path;

import com.google.common.collect.ImmutableList;

import java.util.List;

public enum DependencyTransversalType {
    ALL("all",
            DependencyType.getTransitive(),
            DependencyType.getTransitive()),
    LIBRARY("library",
            ImmutableList.of(DependencyType.LIBRARY),
            ImmutableList.of(DependencyType.LIBRARY, DependencyType.RUNTIME)),
    BUILDTOOLS("tools",
            ImmutableList.of(DependencyType.LIBRARY, DependencyType.BUILDTOOLS),
            ImmutableList.of(DependencyType.LIBRARY, DependencyType.RUNTIME)),
    TEST("test",
            ImmutableList.of(DependencyType.LIBRARY, DependencyType.TEST),
            ImmutableList.of(DependencyType.LIBRARY, DependencyType.RUNTIME)),
    RUNTIME("runtime",
            ImmutableList.of(DependencyType.LIBRARY, DependencyType.RUNTIME),
            ImmutableList.of(DependencyType.LIBRARY, DependencyType.RUNTIME));

    private String name;
    private List<DependencyType> directDependencyTypes;
    private List<DependencyType> transitiveDependencyTypes;

    DependencyTransversalType(String name, ImmutableList<DependencyType> directDependencyTypes, ImmutableList<DependencyType> transitiveDependencyTypes) {
        this.name = name;
        this.transitiveDependencyTypes = transitiveDependencyTypes;
        this.directDependencyTypes = directDependencyTypes;
    }

    public String getName() {
        return name;
    }

    public List<DependencyType> getTransitiveDependencyTypes() {
        return transitiveDependencyTypes;
    }

    public List<DependencyType> getDirectDependencyTypes() {
        return directDependencyTypes;
    }
}
