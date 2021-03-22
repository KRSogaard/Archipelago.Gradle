package build.archipelago.packageservice.core.delegates.getPackageBuildByGit;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import com.github.benmanes.caffeine.cache.Cache;

public class GetPackageBuildByGitDelegate {

    private PackageData packageData;
    private Cache<String, String> publicPackageAccountCache;

    public GetPackageBuildByGitDelegate(PackageData packageData, Cache<String, String> publicPackageAccountCache) {
        this.packageData = packageData;
        this.publicPackageAccountCache = publicPackageAccountCache;
    }

    public ArchipelagoBuiltPackage get(String accountId, String packageName, String gitCommit) throws PackageNotFoundException {
        try {
            return packageData.getBuildPackageByGit(accountId, packageName, gitCommit);
        } catch (PackageNotFoundException exp) {
            try {
                String publicAccountId = publicPackageAccountCache.getIfPresent(packageName.toLowerCase());
                if (publicAccountId == null) {
                    publicAccountId = packageData.getPublicPackage(packageName.toLowerCase());
                    publicPackageAccountCache.put(packageName.toLowerCase(), publicAccountId);
                }
                return packageData.getBuildPackageByGit(publicAccountId, packageName, gitCommit);
            } catch (PackageNotFoundException e) {
                throw exp;
            }
        }
    }
}
