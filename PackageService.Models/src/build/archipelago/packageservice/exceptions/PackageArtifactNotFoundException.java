package build.archipelago.packageservice.exceptions;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.ArchipelagoException;

public class PackageArtifactNotFoundException extends ArchipelagoException {
    public PackageArtifactNotFoundException(ArchipelagoPackage nameVersion, String hash) {
        super("Artifact \"" +
                nameVersion.toString() + "\" with hash [" +
                hash + "] was not found");
    }
}
