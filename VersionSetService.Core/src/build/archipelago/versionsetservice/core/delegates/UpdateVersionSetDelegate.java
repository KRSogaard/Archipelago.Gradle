package build.archipelago.versionsetservice.core.delegates;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.utils.O;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.PackageVerificationResult;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.versionsetservice.core.services.VersionSetService;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.versionsetservice.models.UpdateVersionSetRequest;
import com.google.common.base.*;

import java.util.List;

public class UpdateVersionSetDelegate {

    private VersionSetService versionSetService;
    private PackageServiceClient packageServiceClient;

    public UpdateVersionSetDelegate(VersionSetService versionSetService,
                                    PackageServiceClient packageServiceClient) {
        this.versionSetService = versionSetService;
        this.packageServiceClient = packageServiceClient;
    }

    public void update(String accountId, String versionSetName, UpdateVersionSetRequest request) throws VersionSetDoseNotExistsException,
            PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName));
        Preconditions.checkArgument(request != null);

        // Ensure the version set exists
        versionSetService.get(accountId, versionSetName);

        if (O.isPresent(request.getTarget())) {
            PackageVerificationResult<ArchipelagoPackage> targetsVerified = packageServiceClient.verifyPackagesExists(
                    accountId, List.of(request.getTarget().get()));
            if (!targetsVerified.isValid()) {
                throw new PackageNotFoundException(targetsVerified.getMissingPackages().get(0));
            }
        }

        if (O.isPresent(request.getParent())) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getParent().get()), "The parent attribute was empty or null");
            versionSetService.get(accountId, request.getParent().get());
        }

        versionSetService.update(accountId, versionSetName, request);
    }
}
