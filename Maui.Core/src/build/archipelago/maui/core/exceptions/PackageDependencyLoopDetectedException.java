package build.archipelago.maui.core.exceptions;

import build.archipelago.common.ArchipelagoPackage;

import java.util.*;

public class PackageDependencyLoopDetectedException extends Exception {

    List<ArchipelagoPackage> archipelagoPackages;

    public PackageDependencyLoopDetectedException(ArchipelagoPackage pkg) {
        super();
        archipelagoPackages = List.of(pkg);
    }

    public PackageDependencyLoopDetectedException(Set<ArchipelagoPackage> cycleSet) {
        super();
        archipelagoPackages = List.copyOf(cycleSet);
    }
}