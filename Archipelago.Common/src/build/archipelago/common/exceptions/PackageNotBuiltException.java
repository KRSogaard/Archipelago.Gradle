package build.archipelago.common.exceptions;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.ArchipelagoException;

public class PackageNotBuiltException extends ArchipelagoException {
    public PackageNotBuiltException(ArchipelagoPackage pkg) {
        super("A local package \"" + pkg + "\" is a dependency of the build package, but it has not been built");
    }
}
