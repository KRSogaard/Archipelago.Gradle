package build.archipelago.common.exceptions;

import build.archipelago.common.ArchipelagoPackage;
import lombok.Getter;

@Getter
public class PackageNotInVersionSetException extends ArchipelagoException {

    private String pkgName;

    public PackageNotInVersionSetException(ArchipelagoPackage resolve) {
        super("This package " + resolve.getNameVersion() + " was not found in the version set");
        this.pkgName = resolve.getNameVersion();
    }
    public PackageNotInVersionSetException(String pkg) {
        super("This package " + pkg + " was not found in the version set");
        this.pkgName = pkg;
    }
}
