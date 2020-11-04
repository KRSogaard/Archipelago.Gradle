package build.archipelago.packageservice.core.delegates.getPackageBuildByGit;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.core.data.models.BuiltPackageDetails;

public class GetPackageBuildByGitDelegate {

    private PackageData packageData;

    public GetPackageBuildByGitDelegate(PackageData packageData) {
        this.packageData = packageData;
    }

    public ArchipelagoBuiltPackage get(String accountId, String packageName, String branch, String gitCommit) throws PackageNotFoundException {
        return packageData.getBuildPackageByGit(accountId, packageName, branch, gitCommit);
    }
}
