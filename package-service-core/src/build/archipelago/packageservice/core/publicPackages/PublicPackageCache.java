package build.archipelago.packageservice.core.publicPackages;

import build.archipelago.packageservice.core.data.PackageData;

import java.util.HashMap;
import java.util.Map;

public class PublicPackageCache {
    // TODO: Maybe later
    private Map<String, CacheItem> publicPackages;

    public PublicPackageCache(PackageData packageData) {
        publicPackages = new HashMap<>();
    }

    public boolean isPublicPackage(String name) {
        return publicPackages.containsKey(name.toLowerCase());
    }
    public String getAccountId(String name) {
        CacheItem cache = publicPackages.get(name.toLowerCase());
        if (cache == null) {
            return null;
        }
        return cache.accountId;
    }

    private class CacheItem {
        private String packageName;
        private String accountId;
    }
}
