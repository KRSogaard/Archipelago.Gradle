package build.archipelago.maui.graph;

import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum DependencyType {

    LIBRARY("library", true),
    BUILD("build", true),
    TEST("test", true),
    RUNTIME("runtime", true),
    REMOVE("remove-dependencies", false),
    RESOLVE_CONFLICT("resolve-conflicts", false);

    private String name;
    private boolean transitive;

    private static final ImmutableList<DependencyType> transitiveTypes;
    static {
        transitiveTypes = ImmutableList.copyOf(Arrays.stream(DependencyType.values())
                .filter(DependencyType::isTransitive).collect(Collectors.toList()));
    }

    private DependencyType(String name, boolean isTransitive) {
        this.name = name;
        this.transitive = isTransitive;
    }

    private String getName() {
        return name;
    }

    public boolean isTransitive() {
        return transitive;
    }

    public static ImmutableList<DependencyType> getTransitive() {
        return transitiveTypes;
    }
}
