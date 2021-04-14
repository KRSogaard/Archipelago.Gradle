package build.archipelago.versionsetservice.core.delegates.createVersionSet;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.utils.O;
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

    public void create(CreateVersionSetRequest request)
            throws VersionSetExistsException, PackageNotFoundException, VersionSetDoseNotExistsException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getAccountId()), "An account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getName()), "Name is required");
        Preconditions.checkArgument(NameUtil.validateVersionSetName(request.getName()), "Version set name was invalid");

        try {
            versionSetService.get(request.getAccountId(), request.getName());
            throw new VersionSetExistsException(request.getName());
        } catch (VersionSetDoseNotExistsException exp) {
            // This is expected we do not want the version set to exists
        }

        if (request.getTarget() != null) {
            PackageVerificationResult<ArchipelagoPackage> targetsVerified = packageServiceClient.verifyPackagesExists(
                    request.getAccountId(), List.of(request.getTarget()));
            if (!targetsVerified.isValid()) {
                throw new PackageNotFoundException(targetsVerified.getMissingPackages().get(0));
            }
        }

        if (request.getParent() != null) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getParent()),
                    "Parent is required");
            Preconditions.checkArgument(NameUtil.validateVersionSetName(request.getParent()),
                    "Parent name was not valid");

            // If the parent version set dose not exists this will throw an exception
            versionSetService.get(request.getAccountId(), request.getParent());
        }

        versionSetService.create(request.getAccountId(), request.getName(), request.getTarget(), request.getParent());
    }
}
