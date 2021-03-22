package build.archipelago.packageservice.core.delegates.verifyPackageExists;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class VerifyPackageExistsDelegate {

    private PackageData packageData;
    private Cache<String, Boolean> buildCache;
    private Cache<String, String> publicPackageAccountCache;

    public VerifyPackageExistsDelegate(PackageData packageData, Cache<String, String> publicPackageAccountCache) {
        this.packageData = packageData;
        this.publicPackageAccountCache = publicPackageAccountCache;

        buildCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .build();
    }

    public ImmutableList<ArchipelagoPackage> verify(String accountId, final List<ArchipelagoPackage> packages) {
        Preconditions.checkNotNull(packages, "A list of packages are required");

        var missingPackages = ImmutableList.<ArchipelagoPackage>builder();
        for (ArchipelagoPackage pkg : packages) {
            String cacheKey = accountId + "||" + pkg.getNameVersion();
            if (!buildCache.getIfPresent(cacheKey)) {
                log.debug("Checking if {} exists", pkg);
                if (packageData.getPackageVersionBuilds(accountId, pkg).isEmpty()) {
                    try {
                        String publicAccountId = publicPackageAccountCache.getIfPresent(pkg.getName().toLowerCase());
                        if (publicAccountId == null) {
                            publicAccountId = packageData.getPublicPackage(pkg.getName());
                        }
                        cacheKey = publicAccountId + "||" + pkg.getNameVersion();
                        if (!buildCache.getIfPresent(cacheKey)) {
                            log.debug("Checking if {} exists", pkg);
                            if (packageData.getPackageVersionBuilds(accountId, pkg).isEmpty()) {
                                log.debug("There where no builds for {} so it must not exist", pkg);
                                missingPackages.add(pkg);
                            } else {
                                buildCache.put(cacheKey, true);
                            }
                        }
                    } catch (PackageNotFoundException exp) {
                        log.debug("There where no builds for {} so it must not exist", pkg);
                        missingPackages.add(pkg);
                    }
                } else {
                    buildCache.put(cacheKey, true);
                }
            }
        }
        return missingPackages.build();
    }

}
