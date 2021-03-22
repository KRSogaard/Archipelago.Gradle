package build.archipelago.packageservice.core.delegates.getPackages;

import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.models.PackageDetails;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class GetPackagesDelegate {

    private PackageData packageData;
    private Cache<String, List<PackageDetails>> publicPackagesCache;

    public GetPackagesDelegate(PackageData packageData, Cache<String, List<PackageDetails>> publicPackagesCache) {
        this.packageData = packageData;
        this.publicPackagesCache = publicPackagesCache;
    }

    public ImmutableList<PackageDetails> get(String accountId) {
        ImmutableList.Builder<PackageDetails> returnList = ImmutableList.<PackageDetails>builder();
        returnList.addAll(publicPackagesCache.get("public", k -> packageData.getAllPublicPackages()));
        returnList.addAll(packageData.getAllPackages(accountId));
        return returnList.build();
    }
}
