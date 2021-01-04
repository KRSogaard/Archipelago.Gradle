package build.archipelago.packageservice.core.delegates.getPackage;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.core.data.models.PackageDetails;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class GetPackageDelegate {

    private PackageData packageData;

    public GetPackageDelegate(PackageData packageData) {
        this.packageData = packageData;
    }

    public PackageDetails get(String accountId, String name) throws PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "A package name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "A package name is required");
        Preconditions.checkArgument(ArchipelagoPackage.validateName(name),
                "The package name \"" + name + "\" was not valid");

        return packageData.getPackageDetails(accountId, name);
    }
}
