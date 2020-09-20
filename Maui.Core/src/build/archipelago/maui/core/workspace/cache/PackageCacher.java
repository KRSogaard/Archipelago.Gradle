package build.archipelago.maui.core.workspace.cache;

import build.archipelago.common.ArchipelagoBuiltPackage;

public interface PackageCacher {
    PackageCacheList getCurrentCachedPackages();
    void cache(ArchipelagoBuiltPackage pkg);
}
