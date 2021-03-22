package build.archipelago.packageservice.core.delegates.getPackageBuild;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.models.BuiltPackageDetails;
import com.github.benmanes.caffeine.cache.Cache;

public class GetPackageBuildDelegate {

    private PackageData packageData;
    private Cache<String, String> publicPackageAccountCache;

    public GetPackageBuildDelegate(PackageData packageData, Cache<String, String> publicPackageAccountCache) {
        this.packageData = packageData;
        this.publicPackageAccountCache = publicPackageAccountCache;
    }

    public BuiltPackageDetails get(String accountId, ArchipelagoBuiltPackage pkg) throws PackageNotFoundException {
        try {
            return packageData.getBuildPackage(accountId, pkg);
        } catch (PackageNotFoundException exp) {
            try {
                String publicAccountId = publicPackageAccountCache.getIfPresent(pkg.getName().toLowerCase());
                if (publicAccountId == null) {
                    publicAccountId = packageData.getPublicPackage(pkg.getName().toLowerCase());
                    publicPackageAccountCache.put(pkg.getName().toLowerCase(), publicAccountId);
                }
                return packageData.getBuildPackage(publicAccountId, pkg);
            } catch (PackageNotFoundException e) {
                throw exp;
            }
        }
    }
}
