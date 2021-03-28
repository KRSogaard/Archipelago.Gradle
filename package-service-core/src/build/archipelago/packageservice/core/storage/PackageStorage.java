package build.archipelago.packageservice.core.storage;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;

public interface PackageStorage {
    String getDownloadUrl(String accountId, ArchipelagoBuiltPackage pkg) throws PackageNotFoundException;
    String generatePreSignedUrl(String accountId, ArchipelagoBuiltPackage pkg);
}
