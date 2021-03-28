package build.archipelago.packageservice.core.storage;

import build.archipelago.common.ArchipelagoBuiltPackage;

public interface PackageStorage {
    String getDownloadUrl(String accountId, ArchipelagoBuiltPackage pkg);
    String generatePreSignedUrl(String accountId, ArchipelagoBuiltPackage pkg);
}
