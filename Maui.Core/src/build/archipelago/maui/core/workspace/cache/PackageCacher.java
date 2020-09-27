package build.archipelago.maui.core.workspace.cache;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.exceptions.PackageNotFoundException;

import java.io.IOException;
import java.nio.file.Path;

public interface PackageCacher {
    PackageCacheList getCurrentCachedPackages();
    void cache(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException, IOException;
    Path getCachePath(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException;
}
