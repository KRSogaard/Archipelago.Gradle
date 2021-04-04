package build.archipelago.versionsetservice.core.delegates;

import build.archipelago.common.*;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.PackageVerificationResult;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.versionsetservice.core.services.VersionSetService;
import build.archipelago.versionsetservice.core.utils.NameUtil;
import build.archipelago.versionsetservice.exceptions.*;
import com.google.common.base.*;

import java.util.List;
import java.util.stream.Collectors;

public class CreateVersionSetRevisionDelegate {

    private VersionSetService versionSetService;
    private PackageServiceClient packageServiceClient;

    public CreateVersionSetRevisionDelegate(VersionSetService versionSetService,
                                            PackageServiceClient packageServiceClient) {
        this.versionSetService = versionSetService;
        this.packageServiceClient = packageServiceClient;
    }

    public String createRevision(String accountId, String versionSetName, List<ArchipelagoBuiltPackage> packages, ArchipelagoPackage target)
            throws MissingTargetPackageException, VersionSetDoseNotExistsException, PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "An account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName), "A Version Set name is required");
        Preconditions.checkArgument(NameUtil.validateVersionSetName(versionSetName), "Version set name was invalid");
        Preconditions.checkArgument(packages.size() > 0, "At least 1 package is required for a revision");

        this.validateVersionSetTarget(accountId, versionSetName, packages);
        this.validatePackages(accountId, packages);

        return versionSetService.createRevision(accountId, versionSetName, packages, target);
    }

    private void validateVersionSetTarget(String accountId, String versionSetName, List<ArchipelagoBuiltPackage> packages)
            throws MissingTargetPackageException, VersionSetDoseNotExistsException {

        VersionSet vs = versionSetService.get(accountId, versionSetName);
        if (vs.getTarget() != null) {
            if (packages.stream().noneMatch(p -> p.equals(vs.getTarget()))) {
                throw new MissingTargetPackageException(vs.getTarget());
            }
        }
    }

    private void validatePackages(String accountId, List<ArchipelagoBuiltPackage> parsePackages) throws PackageNotFoundException {
        PackageVerificationResult<ArchipelagoBuiltPackage> pkgVerification =
                packageServiceClient.verifyBuildsExists(accountId, parsePackages);
        if (!pkgVerification.isValid()) {
            // Wierd that i have to do this? The PackageNotFoundException will not accept the ArchipelagoBuiltPackage
            throw new PackageNotFoundException(pkgVerification.getMissingPackages()
                    .stream().map(x -> (ArchipelagoPackage) x).collect(Collectors.toList()));
        }
    }
}
