package build.archipelago.maui.core.workspace;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.concurrent.ExecutorServiceFactory;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.maui.core.workspace.cache.PackageCacheList;
import build.archipelago.maui.core.workspace.cache.PackageCacher;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
public class WorkspaceSyncer {
    private VersionServiceClient vsClient;
    private ExecutorService executor;
    private PackageCacher packageCacher;

    public WorkspaceSyncer(
            PackageCacher packageCacher,
            VersionServiceClient versionSetClient,
            ExecutorServiceFactory executorServiceFactory) {
        this.vsClient = versionSetClient;
        this.executor = executorServiceFactory.create();
        this.packageCacher = packageCacher;
    }

    public void syncVersionSet(String versionSet) throws VersionSetDoseNotExistsException {
        syncVersionSet(versionSet, null);
    }
    public boolean syncVersionSet(String versionSet, String revision) throws VersionSetDoseNotExistsException {
        Instant startTime = Instant.now();
        log.trace("Got request to sync version set: {}", versionSet);
        VersionSet vs = vsClient.getVersionSet(versionSet);
        String rev = vs.getLatestRevision();
        if (revision != null) {
            rev = revision;
        }
        log.trace("Getting packages from version set: {}:{}", vs.getName(), rev);
        VersionSetRevision vsRevision = vsClient.getVersionSetPackages(vs.getName(), rev);

        log.info("Syncing version set {}, with {} packages.", vs.getName(), vsRevision.getPackages().size());

        PackageCacheList cache = packageCacher.getCurrentCachedPackages();
        Set<ArchipelagoBuiltPackage> packagesToSync = vsRevision.getPackages().stream()
                .filter(p -> !cache.hasPackage(p))
                .collect(Collectors.toSet());

        log.info("Syncing {} packages", packagesToSync.size());

        boolean failures = false;
        List<Future> pkgFutures = new ArrayList<>();
        for(ArchipelagoBuiltPackage pkg : packagesToSync) {
            log.trace("Syncing {} to cache", pkg.toString());
            pkgFutures.add(executor.submit(() -> {
                packageCacher.cache(pkg);
            }));
        }
        for (Future future : pkgFutures) {
            try {
                future.get();
            } catch (Exception e) {
                failures = true;
                log.error(e.getMessage(), e);
            }
        }
        Instant endTime = Instant.now();
        Long runTimeMilli = endTime.toEpochMilli() - startTime.toEpochMilli();

        if (failures) {
            log.warn("Syncing version set {} finished with errors in {} ms.", vs.getName(), runTimeMilli);
        } else {
            log.warn("Syncing version set {} finished  in {} ms.", vs.getName(), runTimeMilli);
        }
        return !failures;
    }
}
