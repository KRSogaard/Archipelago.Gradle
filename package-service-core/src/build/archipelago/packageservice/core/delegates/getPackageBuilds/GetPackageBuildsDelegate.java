package build.archipelago.packageservice.core.delegates.getPackageBuilds;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.models.VersionBuildDetails;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetPackageBuildsDelegate {

    private PackageData packageData;

    public GetPackageBuildsDelegate(PackageData packageData) {
        this.packageData = packageData;
    }

    public ImmutableList<VersionBuildDetails> get(String accountId, ArchipelagoPackage pkg) {
        log.info("Get builds for package: {}, account: {}", pkg, accountId);
        return packageData.getPackageVersionBuilds(accountId, pkg);
    }
}
