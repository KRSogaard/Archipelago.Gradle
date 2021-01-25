package build.archipelago.versionsetservice.core.delegates;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.PackageVerificationResult;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.versionsetservice.core.services.VersionSetService;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;
import com.google.common.base.*;

import java.util.Optional;
import java.util.*;

public class UpdateVersionSetDelegate {

    private VersionSetService versionSetService;
    private PackageServiceClient packageServiceClient;

    public UpdateVersionSetDelegate(VersionSetService versionSetService,
                                    PackageServiceClient packageServiceClient) {
        this.versionSetService = versionSetService;
        this.packageServiceClient = packageServiceClient;
    }

    public void update(String accountId, String versionSetName, List<ArchipelagoPackage> targets, Optional<String> parent) throws VersionSetDoseNotExistsException, PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName));
        Preconditions.checkArgument(targets != null);

        versionSetService.get(accountId, versionSetName);

        if (targets.size() > 0) {
            PackageVerificationResult<ArchipelagoPackage> targetsVerified = packageServiceClient.verifyPackagesExists(accountId, targets);
            if (!targetsVerified.isValid()) {
                throw new PackageNotFoundException(targetsVerified.getMissingPackages());
            }
        }

        versionSetService.update(accountId, versionSetName, targets, parent);
    }
}
