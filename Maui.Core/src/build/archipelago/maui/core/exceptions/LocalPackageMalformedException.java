package build.archipelago.maui.core.exceptions;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.ArchipelagoException;

public class LocalPackageMalformedException extends ArchipelagoException {
    public LocalPackageMalformedException(ArchipelagoPackage resolve) {
        super("This package " + resolve.getNameVersion() + " has been checked out, but is malformed");
    }
}