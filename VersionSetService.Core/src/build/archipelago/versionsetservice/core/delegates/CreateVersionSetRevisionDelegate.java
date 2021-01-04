package build.archipelago.versionsetservice.core.delegates;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.MissingTargetPackageException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.PackageVerificationResult;
import build.archipelago.versionsetservice.core.services.VersionSetService;
import build.archipelago.versionsetservice.core.utils.NameUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

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

    public String createRevision(String accountId, String versionSetName, List<ArchipelagoBuiltPackage> packages)
            throws MissingTargetPackageException, VersionSetDoseNotExistsException, PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "An account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName), "A Version Set name is required");
        Preconditions.checkArgument(NameUtil.validateVersionSetName(versionSetName), "Version set name was invalid");
        Preconditions.checkArgument(packages.size() > 0, "At least 1 package is required for a revision");

        validateVersionSetTargets(accountId, versionSetName, packages);
        validatePackages(accountId, packages);

        return versionSetService.createRevision(accountId, versionSetName, packages);
    }

    private void validateVersionSetTargets(String accountId, String versionSetName, List<ArchipelagoBuiltPackage> packages)
            throws MissingTargetPackageException, VersionSetDoseNotExistsException {
        VersionSet vs = versionSetService.get(accountId, versionSetName);
        for (ArchipelagoPackage pkg : vs.getTargets()) {
            if (packages.stream().noneMatch(p ->
                    pkg.getName().equalsIgnoreCase(p.getName()) &&
                            pkg.getVersion().equalsIgnoreCase(p.getVersion()))) {
                throw new MissingTargetPackageException(pkg);
            }
        }
    }

    private void validatePackages(String accountId, List<ArchipelagoBuiltPackage> parsePackages) throws PackageNotFoundException {
        PackageVerificationResult<ArchipelagoBuiltPackage> pkgVerification =
                packageServiceClient.verifyBuildsExists(accountId, parsePackages);
        if (!pkgVerification.isValid()) {
            // Wierd that i have to do this? The PackageNotFoundException will not accept the ArchipelagoBuiltPackage
            List<ArchipelagoPackage> packages = pkgVerification.getMissingPackages()
                    .stream().map(x -> (ArchipelagoPackage)x).collect(Collectors.toList());
            throw new PackageNotFoundException(packages);
        }
    }
}
