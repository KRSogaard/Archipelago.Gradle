package build.archipelago.common.exceptions;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.ArchipelagoException;

public class PackageNotInVersionSetException extends ArchipelagoException {
    public PackageNotInVersionSetException(ArchipelagoPackage resolve) {
        super("This package " + resolve.getNameVersion() + " was not found in the version set");
    }
}
