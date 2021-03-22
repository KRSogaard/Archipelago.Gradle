package build.archipelago.packageservice.core.delegates.getBuildArtifact;

import build.archipelago.common.*;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.core.storage.PackageStorage;
import build.archipelago.packageservice.core.utils.Constants;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GetBuildArtifactDelegate {

    private PackageStorage packageStorage;
    private PackageData packageData;
    private Cache<String, String> publicPackageAccountCache;

    public GetBuildArtifactDelegate(PackageData packageData,
                                    PackageStorage packageStorage,
                                    Cache<String, String> publicPackageAccountCache) {
        this.packageData = packageData;
        this.packageStorage = packageStorage;
        this.publicPackageAccountCache = publicPackageAccountCache;
    }

    public GetBuildArtifactResponse getBuildArtifact(String accountId, ArchipelagoPackage nameVersion, Optional<String> hash)
            throws IOException, PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkNotNull(nameVersion, "Name required");
        Preconditions.checkNotNull(hash, "A hash is required");

        Pair<ArchipelagoBuiltPackage, String> pair;
        try {
            pair = getPackageBuild(accountId, nameVersion, hash);
        } catch (PackageNotFoundException exp) {
            try {
                String publicAccountId = publicPackageAccountCache.getIfPresent(nameVersion.getName().toLowerCase());
                if (publicAccountId == null) {
                    publicAccountId = packageData.getPublicPackage(nameVersion.getName());
                    publicPackageAccountCache.put(nameVersion.getName(), publicAccountId);
                }
                pair = getPackageBuild(publicAccountId, nameVersion, hash);
            } catch (PackageNotFoundException e) {
                throw exp;
            }
        }

        return GetBuildArtifactResponse.builder()
                .downloadUrl(pair.getRight())
                .pkg(pair.getLeft())
                .build();
    }

    private Pair<ArchipelagoBuiltPackage, String> getPackageBuild(
            String accountId, ArchipelagoPackage nameVersion, Optional<String> hash)
            throws PackageNotFoundException {
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
        return new ImmutablePair<>(pkg, packageStorage.getDownloadUrl(accountId, pkg));
    }
}
