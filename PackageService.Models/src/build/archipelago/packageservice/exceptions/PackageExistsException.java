package build.archipelago.packageservice.exceptions;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.ArchipelagoException;
import lombok.Getter;

@Getter
public class PackageExistsException extends ArchipelagoException {

    private String packageName;
    private String version;
    private String hash;

    public PackageExistsException(String name) {
        super("The package \"" + name + "\" already exists");
        this.packageName = name;
    }

    public PackageExistsException(ArchipelagoBuiltPackage pkg) {
        super("The package \"" + pkg + "\" already exists");
        this.packageName = pkg.getName();
        this.version = pkg.getVersion();
        this.hash = pkg.getHash();
    }

    public PackageExistsException(ArchipelagoPackage pkg) {
        super("The package \"" + pkg.getNameVersion() + "\" already exists");
        this.packageName = pkg.getName();
        this.version = pkg.getVersion();
    }
}
