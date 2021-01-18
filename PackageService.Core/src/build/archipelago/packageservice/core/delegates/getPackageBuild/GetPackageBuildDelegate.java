package build.archipelago.packageservice.core.delegates.getPackageBuild;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.models.BuiltPackageDetails;

public class GetPackageBuildDelegate {

    private PackageData packageData;

    public GetPackageBuildDelegate(PackageData packageData) {
        this.packageData = packageData;
    }

    public BuiltPackageDetails get(String accountId, ArchipelagoBuiltPackage pkg) throws PackageNotFoundException {
        return packageData.getBuildPackage(accountId, pkg);
    }
}
