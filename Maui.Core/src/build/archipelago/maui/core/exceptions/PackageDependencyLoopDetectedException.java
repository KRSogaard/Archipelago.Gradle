package build.archipelago.maui.core.exceptions;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.ArchipelagoException;

import java.util.*;

public class PackageDependencyLoopDetectedException extends ArchipelagoException {

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