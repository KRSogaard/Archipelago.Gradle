package build.archipelago.packageservice.core.delegates.getPackageBuilds;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.models.VersionBuildDetails;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetPackageBuildsDelegate {

    private PackageData packageData;
    private Cache<String, String> publicPackageAccountCache;

    public GetPackageBuildsDelegate(PackageData packageData, Cache<String, String> publicPackageAccountCache) {
        this.packageData = packageData;
        this.publicPackageAccountCache = publicPackageAccountCache;
    }

    public ImmutableList<VersionBuildDetails> get(String accountId, ArchipelagoPackage pkg) throws PackageNotFoundException {
        log.info("Get builds for package: {}, account: {}", pkg, accountId);
        try {
            packageData.getPackageDetails(accountId, pkg.getName());
            return packageData.getPackageVersionBuilds(accountId, pkg);
        } catch (PackageNotFoundException exp) {
            log.debug("Did not find the package, trying public packages");
            String publicAccountId = publicPackageAccountCache.getIfPresent(pkg.getName().toLowerCase());
            if (publicAccountId == null) {
                publicAccountId = packageData.getPublicPackage(pkg.getName().toLowerCase());
                publicPackageAccountCache.put(pkg.getName().toLowerCase(), publicAccountId);
            }
            return packageData.getPackageVersionBuilds(publicAccountId, pkg);
        }
    }
}
