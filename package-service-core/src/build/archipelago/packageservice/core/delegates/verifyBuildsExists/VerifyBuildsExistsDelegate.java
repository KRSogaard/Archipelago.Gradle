package build.archipelago.packageservice.core.delegates.verifyBuildsExists;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.models.PackageDetails;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class VerifyBuildsExistsDelegate {

    private PackageData packageData;
    private Cache<String, Boolean> buildCache;
    private Cache<String, String> publicPackageAccountCache;

    public VerifyBuildsExistsDelegate(PackageData packageData, Cache<String, String> publicPackageAccountCache) {
        this.packageData = packageData;
        this.publicPackageAccountCache = publicPackageAccountCache;

        buildCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .build();
    }

    public ImmutableList<ArchipelagoBuiltPackage> verify(String accountId, List<ArchipelagoBuiltPackage> packages) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(packages);

        var missingPackages = ImmutableList.<ArchipelagoBuiltPackage>builder();
        for (ArchipelagoBuiltPackage pkg : packages) {
            try {
                String cacheKey = accountId + "||" + pkg.getBuiltPackageName();
                if (buildCache.getIfPresent(cacheKey) == null) {
                    log.debug("Checking if {} exists", pkg);
                    packageData.getBuildPackage(accountId, pkg);
                    buildCache.put(cacheKey, true);
                }
            } catch (PackageNotFoundException e) {
                try {
                    String publicAccountId = publicPackageAccountCache.getIfPresent(pkg.getName().toLowerCase());
                    if (publicAccountId == null) {
                        publicAccountId = packageData.getPublicPackage(pkg.getName());
                    }
                    String cacheKey = publicAccountId + "||" + pkg.getBuiltPackageName();
                    if (buildCache.getIfPresent(cacheKey) == null) {
                        log.debug("Checking if public package {} exists", pkg);
                        packageData.getBuildPackage(publicAccountId, pkg);
                        buildCache.put(cacheKey, true);
                    }
                } catch (PackageNotFoundException exp) {
                    log.debug("Got not found exception, {}", e.getMessage(), e);
                    missingPackages.add(pkg);
                }
            }
        }
        return missingPackages.build();
    }
}
