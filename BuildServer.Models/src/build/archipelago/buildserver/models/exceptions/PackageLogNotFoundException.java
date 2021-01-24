package build.archipelago.buildserver.models.exceptions;

import build.archipelago.common.ArchipelagoPackage;
import lombok.*;

@Builder
@Getter
public class PackageLogNotFoundException extends Exception {
    private String buildId;
    private ArchipelagoPackage pkg;

    public PackageLogNotFoundException(String buildId, ArchipelagoPackage pkg) {
        super("Package build log for '" + pkg.toString() + "' in build '" + buildId + "' was not found");
        this.buildId = buildId;
        this.pkg = pkg;
    }
}
