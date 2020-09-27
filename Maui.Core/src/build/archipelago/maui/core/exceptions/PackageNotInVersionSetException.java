package build.archipelago.maui.core.exceptions;

import build.archipelago.common.ArchipelagoPackage;

public class PackageNotInVersionSetException extends Exception {
    public PackageNotInVersionSetException(ArchipelagoPackage resolve) {
        super("This package " + resolve.getNameVersion() + " was not found in the version set");
    }
}
