package build.archipelago.packageservice.exceptions;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.ArchipelagoException;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class PackageNotFoundException extends ArchipelagoException {

    private String packageName;
    private String version;
    private String hash;
    private List<ArchipelagoPackage> packages;

    public PackageNotFoundException(String name) {
        super(getMessage(name));
        this.packageName = name;
    }

    public PackageNotFoundException(ArchipelagoPackage pkg) {
        super(getMessage(pkg));
        this.packageName = pkg.getNameVersion();
        this.version = pkg.getVersion();
    }

    public PackageNotFoundException(ArchipelagoBuiltPackage pkg) {
        super(getMessage(pkg));
        this.packageName = pkg.getBuiltPackageName();
        this.version = pkg.getVersion();
        this.hash = pkg.getHash();
    }

    public PackageNotFoundException(List<ArchipelagoPackage> pkgs) {
        super(getMessage(pkgs));
        packages = pkgs;
    }

    private static String getMessage(String name) {
        return String.format("The package \"%s\" was not found", name);
    }

    private static String getMessage(ArchipelagoPackage pkg) {
        return String.format("The package \"%s\" was not found", pkg.toString());
    }

    private static String getMessage(List<ArchipelagoPackage> pkgs) {
        return String.format("The packages [%s] was not found",
                pkgs.stream().map(x -> x.toString()).collect(Collectors.joining(",")));
    }
}
