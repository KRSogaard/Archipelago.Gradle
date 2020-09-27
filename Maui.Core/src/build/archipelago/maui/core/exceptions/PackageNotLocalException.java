package build.archipelago.maui.core.exceptions;

import build.archipelago.common.ArchipelagoPackage;

public class PackageNotLocalException  extends Exception {
    public PackageNotLocalException(ArchipelagoPackage resolve) {
        super("This package " + resolve.getNameVersion() + " has not been checked out in the workspace");
    }
}