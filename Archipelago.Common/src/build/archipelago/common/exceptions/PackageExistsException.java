package build.archipelago.common.exceptions;

import build.archipelago.common.*;

public class PackageExistsException extends ArchipelagoException {
    public PackageExistsException(String name) {
        super("The package \"" + name + "\" already exists");
    }

    public PackageExistsException(ArchipelagoBuiltPackage pkg) {
        super("The package \"" + pkg + "\" already exists");
    }

    public PackageExistsException(ArchipelagoPackage pkg) {
        super("The package \"" + pkg.getNameVersion() + "\" already exists");
    }
}
