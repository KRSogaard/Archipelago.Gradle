package build.archipelago.packageservice.core.storage;

import build.archipelago.common.ArchipelagoBuiltPackage;

import java.io.IOException;

public interface PackageStorage {
    void upload(String accountId, ArchipelagoBuiltPackage pkg, byte[] artifactBytes);
    byte[] get(String accountId, ArchipelagoBuiltPackage pkg) throws IOException;
}
