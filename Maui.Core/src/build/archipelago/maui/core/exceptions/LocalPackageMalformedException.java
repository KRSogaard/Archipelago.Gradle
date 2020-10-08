package build.archipelago.maui.core.exceptions;

import build.archipelago.common.ArchipelagoPackage;

public class LocalPackageMalformedException extends Exception {
    public LocalPackageMalformedException(ArchipelagoPackage resolve) {
        super("This package " + resolve.getNameVersion() + " has been checked out, but is malformed");
    }
}