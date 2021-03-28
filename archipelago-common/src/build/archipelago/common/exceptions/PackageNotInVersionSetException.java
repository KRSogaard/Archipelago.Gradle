package build.archipelago.common.exceptions;

import build.archipelago.common.ArchipelagoPackage;

public class PackageNotInVersionSetException extends ArchipelagoException {
    public PackageNotInVersionSetException(ArchipelagoPackage resolve) {
        super("This package " + resolve.getNameVersion() + " was not found in the version set");
    }
    public PackageNotInVersionSetException(String pkg) {
        super("This package " + pkg + " was not found in the version set");
    }
}
