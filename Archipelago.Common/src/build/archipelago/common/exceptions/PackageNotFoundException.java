package build.archipelago.common.exceptions;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

public class PackageNotFoundException extends ArchipelagoException {

    @Getter
    private String packageName;

    public PackageNotFoundException(String name) {
        super(getMessage(name));
        packageName = name;
    }
    public PackageNotFoundException(ArchipelagoPackage pkg) {
        super(getMessage(pkg));
        packageName = pkg.getNameVersion();
    }

    public PackageNotFoundException(ArchipelagoBuiltPackage pkg) {
        super(getMessage(pkg));
        packageName = pkg.getBuiltPackageName();
    }

    public PackageNotFoundException(List<ArchipelagoPackage> pkgs) {
        super(getMessage(pkgs));
        packageName = pkgs.stream().map(ArchipelagoPackage::getNameVersion).collect(Collectors.joining(","));
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
