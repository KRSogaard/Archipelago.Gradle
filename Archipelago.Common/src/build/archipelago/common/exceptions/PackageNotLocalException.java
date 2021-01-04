package build.archipelago.common.exceptions;

import build.archipelago.common.ArchipelagoPackage;

public class PackageNotLocalException extends ArchipelagoException {
    public PackageNotLocalException(ArchipelagoPackage resolve) {
        super("This package " + resolve.getNameVersion() + " has not been checked out in the workspace");
    }
}