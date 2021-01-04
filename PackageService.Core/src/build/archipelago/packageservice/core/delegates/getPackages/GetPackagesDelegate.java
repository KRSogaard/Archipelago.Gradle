package build.archipelago.packageservice.core.delegates.getPackages;

import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.core.data.models.PackageDetails;
import com.google.common.collect.ImmutableList;

public class GetPackagesDelegate {

    private PackageData packageData;

    public GetPackagesDelegate(PackageData packageData) {
        this.packageData = packageData;
    }

    public ImmutableList<PackageDetails> get(String accountId) {
        return packageData.getAllPackages(accountId);
    }
}
