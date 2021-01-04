package build.archipelago.packageservice.core.delegates.getBuildArtifact;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.core.storage.PackageStorage;
import build.archipelago.packageservice.core.utils.Constants;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;

@Slf4j
public class GetBuildArtifactDelegate {

    private PackageStorage packageStorage;
    private PackageData packageData;

    public GetBuildArtifactDelegate(PackageData packageData,
                                 PackageStorage packageStorage) {
        this.packageData = packageData;
        this.packageStorage = packageStorage;
    }

    public GetBuildArtifactResponse getBuildArtifact(String accountId, ArchipelagoPackage nameVersion, Optional<String> hash)
            throws IOException, PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkNotNull(nameVersion, "Name required");
        Preconditions.checkNotNull(hash, "A hash is required");

        String latestHash = hash.orElse(null);
        if (hash.isEmpty() || Constants.LATEST.equalsIgnoreCase(hash.get())) {
            try {
                latestHash = packageData.getLatestBuildPackage(accountId, nameVersion).getHash();
            } catch (PackageNotFoundException exp) {
                log.info("Was not able to find latest hash for package {}", nameVersion.toString());
                throw exp;
            }
        }
        ArchipelagoBuiltPackage pkg = new ArchipelagoBuiltPackage(nameVersion, latestHash);

        return GetBuildArtifactResponse.builder()
                .byteArray(packageStorage.get(accountId, pkg))
                .pkg(pkg)
                .build();
    }
}
