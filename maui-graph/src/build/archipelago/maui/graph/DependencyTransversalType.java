package build.archipelago.maui.graph;

import com.google.common.collect.ImmutableList;

import java.util.List;

public enum DependencyTransversalType {
    PKG("pkg",
            true,
            ImmutableList.of(),
            ImmutableList.of()),
    ALL("all",
            true,
            DependencyType.getTransitive(),
            DependencyType.getTransitive()),
    LIBRARY("library",
            false,
            ImmutableList.of(DependencyType.LIBRARY),
            ImmutableList.of(DependencyType.LIBRARY, DependencyType.RUNTIME)),
    LIBRARYDIRECT("library-direct",
            false,
            ImmutableList.of(DependencyType.LIBRARY),
            ImmutableList.of()),
    BUILD("build",
            false,
            ImmutableList.of(DependencyType.LIBRARY, DependencyType.BUILD),
            ImmutableList.of(DependencyType.LIBRARY, DependencyType.RUNTIME)),
    TEST("test",
            false,
            ImmutableList.of(DependencyType.LIBRARY, DependencyType.TEST),
            ImmutableList.of(DependencyType.LIBRARY, DependencyType.RUNTIME)),
    TESTDIRECT("test-direct",
            false,
            ImmutableList.of(DependencyType.LIBRARY, DependencyType.TEST),
            ImmutableList.of()),
    RUNTIME("runtime",
            false,
            ImmutableList.of(DependencyType.LIBRARY, DependencyType.RUNTIME),
            ImmutableList.of(DependencyType.LIBRARY, DependencyType.RUNTIME)),


    BUILD_TOOLS("build-tools",
            false,
            ImmutableList.of(DependencyType.LIBRARY, DependencyType.BUILD, DependencyType.TEST),
            ImmutableList.of(DependencyType.LIBRARY, DependencyType.RUNTIME));

    private String name;
    private boolean includeRoot;
    private List<DependencyType> directDependencyTypes;
    private List<DependencyType> transitiveDependencyTypes;

    DependencyTransversalType(String name, boolean includeRoot, ImmutableList<DependencyType> directDependencyTypes, ImmutableList<DependencyType> transitiveDependencyTypes) {
        this.name = name;
        this.includeRoot = includeRoot;
        this.transitiveDependencyTypes = transitiveDependencyTypes;
        this.directDependencyTypes = directDependencyTypes;
    }

    public String getName() {
        return name;
    }

    public boolean includeRoot() { return includeRoot; }

    public List<DependencyType> getTransitiveDependencyTypes() {
        return transitiveDependencyTypes;
    }

    public List<DependencyType> getDirectDependencyTypes() {
        return directDependencyTypes;
    }
}
