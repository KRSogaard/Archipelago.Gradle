package build.archipelago.maui.common.cache;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;

import java.nio.file.Path;

public interface PackageCacher {
    PackageCacheList getCurrentCachedPackages();
    void cache(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException;
    Path getCachePath(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException;
}
