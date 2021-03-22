package build.archipelago.packageservice.core.delegates.getPackage;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.models.PackageDetails;
import com.amazonaws.util.Base64;
import com.google.common.base.*;
import com.github.benmanes.caffeine.cache.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GetPackageDelegate {

    private PackageData packageData;
    private Cache<String, PackageDetails> privatePackageCache;
    private Cache<String, PackageDetails> publicPackageCache;
    private Cache<String, String> publicPackageAccountCache;

    public GetPackageDelegate(PackageData packageData, Cache<String, String> publicPackageAccountCache) {
        this.packageData = packageData;
        this.publicPackageAccountCache = publicPackageAccountCache;
        publicPackageCache = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
        privatePackageCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }

    public PackageDetails get(String accountId, String name) throws PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "A package name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "A package name is required");
        Preconditions.checkArgument(ArchipelagoPackage.validateName(name),
                "The package name \"" + name + "\" was not valid");
        PackageDetails pkg;
        try {
            String cacheKey = getCacheKey(accountId, name);
            pkg = privatePackageCache.getIfPresent(cacheKey);
            if (pkg != null) {
                return pkg;
            }
            pkg = packageData.getPackageDetails(accountId, name);
            privatePackageCache.put(cacheKey, pkg);
            return pkg;
        } catch (PackageNotFoundException exp) {
            log.debug("Package '{}' was not found, checking public packages", name);
            try {
                String cacheKey = getCacheKey("public", name);
                pkg = publicPackageCache.getIfPresent(cacheKey);
                if (pkg != null) {
                    log.info("Using cached public package '{}' from account '{}'", pkg.getName(), pkg.getOwner());
                    return pkg;
                }
                String publicAccountId = publicPackageAccountCache.getIfPresent(name.toLowerCase());
                if (publicAccountId == null) {
                    publicAccountId = packageData.getPublicPackage(name);
                    publicPackageAccountCache.put(name, publicAccountId);
                }
                log.info("Using public package '{}' from account '{}'", name, publicAccountId);
                pkg = packageData.getPackageDetails(publicAccountId, name);

                publicPackageCache.put(cacheKey, pkg);
                return pkg;
            } catch (PackageNotFoundException e) {
                // Throw the original exception
                throw exp;
            }
        }
    }

    private String getCacheKey(String accountId, String name) {
        return Base64.encodeAsString((accountId + "||\\" + name).toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
