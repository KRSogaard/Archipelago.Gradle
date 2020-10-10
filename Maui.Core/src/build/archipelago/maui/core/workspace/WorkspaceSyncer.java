package build.archipelago.maui.core.workspace;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.concurrent.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.versionset.*;
import build.archipelago.maui.core.workspace.cache.*;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class WorkspaceSyncer {
    private VersionSetServiceClient vsClient;
    private ExecutorService executor;
    private PackageCacher packageCacher;

    public WorkspaceSyncer(
            PackageCacher packageCacher,
            VersionSetServiceClient versionSetClient,
            ExecutorServiceFactory executorServiceFactory) {
        this.vsClient = versionSetClient;
        this.executor = executorServiceFactory.create();
        this.packageCacher = packageCacher;
    }

    public VersionSetRevision syncVersionSet(String versionSet) throws VersionSetDoseNotExistsException {
        return syncVersionSet(versionSet, null);
    }
    public VersionSetRevision syncVersionSet(String versionSet, String revision) throws VersionSetDoseNotExistsException {
        Instant startTime = Instant.now();
        log.trace("Got request to sync version set: {}", versionSet);
        VersionSet vs = vsClient.getVersionSet(versionSet);
        String rev = vs.getLatestRevision();
        if (revision != null) {
            rev = revision;
        }
        log.trace("Getting packages from version set: {}:{}", vs.getName(), rev);
        VersionSetRevision vsRevision = vsClient.getVersionSetPackages(vs.getName(), rev);

        log.info("Syncing version set {}, with {} packages in the revision.", vs.getName(), vsRevision.getPackages().size());

        final Wrap<Boolean> failures = new Wrap<>(false);
        PackageCacheList cache = packageCacher.getCurrentCachedPackages();
        Set<ArchipelagoBuiltPackage> packagesToSync = vsRevision.getPackages().stream()
                .filter(p -> {
                    try {
                        return !cache.hasPackage(p);
                    } catch (Exception e) {
                        failures.setValue(true);
                        log.error(e.getMessage(), e);
                        return false;
                    }
                })
                .collect(Collectors.toSet());
        log.info("Syncing {} packages", packagesToSync.size());

        List<Future> pkgFutures = new ArrayList<>();
        for(ArchipelagoBuiltPackage pkg : packagesToSync) {
            log.trace("Syncing {} to cache", pkg.toString());
            pkgFutures.add(executor.submit(() -> {
                try {
                    packageCacher.cache(pkg);
                } catch (PackageNotFoundException e) {
                    log.error(e.getMessage(), e);
                    failures.setValue(false);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    failures.setValue(false);
                }
            }));
        }
        for (Future future : pkgFutures) {
            try {
                future.get();
            } catch (Exception e) {
                failures.setValue(true);
                log.error(e.getMessage(), e);
            }
        }
        Instant endTime = Instant.now();
        Long runTimeMilli = endTime.toEpochMilli() - startTime.toEpochMilli();

        if (failures.getValue()) {
            log.warn("Syncing version set {} finished with errors in {} ms.", vs.getName(), runTimeMilli);
        } else {
            log.warn("Syncing version set {} finished  in {} ms.", vs.getName(), runTimeMilli);
        }
        if (failures.getValue()) {
            return null;
        }
        return vsRevision;
    }
}
