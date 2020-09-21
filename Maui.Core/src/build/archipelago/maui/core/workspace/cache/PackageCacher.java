package build.archipelago.maui.core.workspace.cache;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.exceptions.PackageNotFoundException;

import java.io.IOException;

public interface PackageCacher {
    PackageCacheList getCurrentCachedPackages();
    void cache(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException, IOException;
}
