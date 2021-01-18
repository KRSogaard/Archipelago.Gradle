package build.archipelago.versionsetservice.exceptions;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.ArchipelagoException;
import lombok.Getter;

@Getter
public class MissingTargetPackageException extends ArchipelagoException {

    private String packageName;
    private String version;

    public MissingTargetPackageException(ArchipelagoPackage pkg) {
        super("This target package " + pkg.toString() +
                " was not in the build packages list, all Version Set targets is" +
                " required to be in the version set revision");
        this.packageName = pkg.getName();
        this.version = pkg.getVersion();
    }
}
