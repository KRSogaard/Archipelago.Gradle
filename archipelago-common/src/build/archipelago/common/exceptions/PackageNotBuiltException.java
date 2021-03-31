package build.archipelago.common.exceptions;

import build.archipelago.common.ArchipelagoPackage;
import lombok.Getter;

@Getter
public class PackageNotBuiltException extends ArchipelagoException {
    private ArchipelagoPackage pkg;

    public PackageNotBuiltException(ArchipelagoPackage pkg) {
        super("A local package \"" + pkg + "\" is a dependency of the build package, but it has not been built");
        this.pkg = pkg;
    }
}
