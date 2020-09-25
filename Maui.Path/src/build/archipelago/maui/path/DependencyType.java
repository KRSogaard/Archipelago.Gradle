package build.archipelago.maui.path;

import java.util.List;

public enum DependencyType {

    LIBRARY("library"),
    BUILDTOOLS("build-tools"),
    TEST("test"),
    RUNTIME("runtime"),
    REMOVE("remove-dependencies"),
    RESOLVE_CONFLICT("resolve-conflicts");


    private DependencyType(String name) {

    }
}
