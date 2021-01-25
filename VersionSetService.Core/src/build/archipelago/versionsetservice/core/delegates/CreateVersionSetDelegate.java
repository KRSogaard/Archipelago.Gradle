package build.archipelago.versionsetservice.core.delegates;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.PackageVerificationResult;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.versionsetservice.core.services.VersionSetService;
import build.archipelago.versionsetservice.core.utils.NameUtil;
import build.archipelago.versionsetservice.exceptions.*;
import com.google.common.base.*;

import java.util.Optional;
import java.util.*;

public class CreateVersionSetDelegate {

    private VersionSetService versionSetService;
    private PackageServiceClient packageServiceClient;

    public CreateVersionSetDelegate(VersionSetService versionSetService,
                                    PackageServiceClient packageServiceClient) {
        this.versionSetService = versionSetService;
        this.packageServiceClient = packageServiceClient;
    }

    public void create(String accountId, String name, List<ArchipelagoPackage> targets, Optional<String> parent)
            throws VersionSetExistsException, PackageNotFoundException, VersionSetDoseNotExistsException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "An account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Name is required");
        Preconditions.checkArgument(NameUtil.validateVersionSetName(name), "Version set name was invalid");

        try {
            versionSetService.get(accountId, name);
            throw new VersionSetExistsException(name);
        } catch (VersionSetDoseNotExistsException exp) {
            // This is expected we do not want the version set to exists
        }

        if (targets.size() > 0) {
            PackageVerificationResult<ArchipelagoPackage> targetsVerified = packageServiceClient.verifyPackagesExists(accountId, targets);
            if (!targetsVerified.isValid()) {
                throw new PackageNotFoundException(targetsVerified.getMissingPackages());
            }
        }

        if (parent.isPresent()) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(parent.get()),
                    "Parent is required");
            Preconditions.checkArgument(NameUtil.validateVersionSetName(parent.get()),
                    "Parent name was not valid");

            // If the parent version set dose not exists this will throw an exception
            versionSetService.get(accountId, parent.get());
        }

        versionSetService.create(accountId, name, targets, parent);
    }
}
