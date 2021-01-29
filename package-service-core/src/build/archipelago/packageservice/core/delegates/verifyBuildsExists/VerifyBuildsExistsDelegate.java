package build.archipelago.packageservice.core.delegates.verifyBuildsExists;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class VerifyBuildsExistsDelegate {

    private PackageData packageData;

    public VerifyBuildsExistsDelegate(PackageData packageData) {
        this.packageData = packageData;
    }

    public ImmutableList<ArchipelagoBuiltPackage> verify(String accountId, List<ArchipelagoBuiltPackage> packages) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(packages);

        var missingPackages = ImmutableList.<ArchipelagoBuiltPackage>builder();
        for (ArchipelagoBuiltPackage pkg : packages) {
            try {
                log.debug("Checking if {} exists", pkg);
                packageData.getBuildPackage(accountId, pkg);
            } catch (PackageNotFoundException e) {
                log.debug("Got not found exception, {}", e.getMessage(), e);
                missingPackages.add(pkg);
            }
        }
        return missingPackages.build();
    }
}
