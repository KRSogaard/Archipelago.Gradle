package build.archipelago.common.exceptions;

import build.archipelago.common.ArchipelagoPackage;

public class LocalPackageMalformedException extends ArchipelagoException {
    public LocalPackageMalformedException(ArchipelagoPackage resolve) {
        super("This package " + resolve.getNameVersion() + " has been checked out, but is malformed");
    }
}