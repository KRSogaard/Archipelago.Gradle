package build.archipelago.maui.core.exceptions;

import build.archipelago.common.ArchipelagoPackage;

public class PackageNotBuiltException extends Exception {
    public PackageNotBuiltException(ArchipelagoPackage pkg) {
        super("A local package \"" + pkg + "\" is a dependency of the build package, but it has not been built");
    }
}
