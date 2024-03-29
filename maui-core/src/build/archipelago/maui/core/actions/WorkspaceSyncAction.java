package build.archipelago.maui.core.actions;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.concurrent.Wrap;
import build.archipelago.common.versionset.*;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.common.cache.*;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class WorkspaceSyncAction extends BaseAction {

    private PackageCacher packageCacher;
    private HarborClient harborClient;
    private ExecutorService executor;

    public WorkspaceSyncAction(WorkspaceContextFactory workspaceContextFactory,
                               SystemPathProvider systemPathProvider,
                               OutputWrapper out,
                               HarborClient harborClient,
                               PackageCacher packageCacher,
                               ExecutorService executor) {
        super(workspaceContextFactory, systemPathProvider, out);
        this.packageCacher = packageCacher;
        this.harborClient = harborClient;
        this.executor = executor;
    }


    public boolean syncWorkspace(String revision) {
        if (!this.setupWorkspaceContext()) {
            out.error("Was unable to locate the workspace");
            return false;
        }

        if (workspaceContext.getVersionSet() == null) {
            out.write("No version set is assigned to the workspace, can not sync.");
            return false;
        }

        if (revision != null) {
            revision = revision.trim();
        }

        VersionSet vs;
        VersionSetRevision versionSetRevision;
        try {
            vs = harborClient.getVersionSet(workspaceContext.getVersionSet());
            if (revision == null) {
                if (vs.getLatestRevision() == null) {
                    log.warn("There are no latest revision for this version-set it must be a new version-set, can't sync");
                    return false;
                }
                revision = vs.getLatestRevision();
                log.info("No revision was provided for the sync of {}, using latest {}", vs.getName(), revision);
            }
            versionSetRevision = harborClient.getVersionSetRevision(workspaceContext.getVersionSet(), revision);
        } catch (VersionSetDoseNotExistsException e) {
            log.error(String.format("Was unable to sync workspace \"%s\" as the version set \"%s#%s\" dose not exists",
                    wsDir.toString(), workspaceContext.getVersionSet(), revision), e);
            if (revision != null) {
                out.error("Was unable to sync the workspace as the version set \"%s#%s\" dose not exists",
                        workspaceContext.getVersionSet(), revision);
            } else {
                out.error("Was unable to sync the workspace as the version set \"%s\" dose not exists",
                        workspaceContext.getVersionSet());
            }
            return false;
        }

        if (!this.syncVersionSet(vs, versionSetRevision)) {
            return false;
        }
        workspaceContext.saveRevisionCache(versionSetRevision);

        return true;
    }

    private boolean syncVersionSet(VersionSet vs, VersionSetRevision vsRevision) {
        Preconditions.checkNotNull(vs);
        Preconditions.checkNotNull(vsRevision);

        Instant startTime = Instant.now();
        log.trace("Getting packages from version set: {}", vs.getName());

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
        for (ArchipelagoBuiltPackage pkg : packagesToSync) {
            log.trace("Syncing {} to cache", pkg.toString());
            out.write("Syncing %s", pkg.toString());
            pkgFutures.add(executor.submit(() -> {
                try {
                    packageCacher.cache(pkg);
                } catch (PackageNotFoundException e) {
                    log.error(e.getMessage(), e);
                    out.error("Failed to syncing %s", pkg.toString());
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
            out.error("Syncing version set %s finished with errors in %s ms.", vs.getName(), runTimeMilli.toString());
            log.warn("Syncing version set {} finished with errors in {} ms.", vs.getName(), runTimeMilli);
        } else {
            out.error("Syncing version set %s finished  in %s ms.", vs.getName(), runTimeMilli.toString());
            log.warn("Syncing version set {} finished  in {} ms.", vs.getName(), runTimeMilli);
        }
        if (failures.getValue()) {
            return false;
        }
        return true;
    }
}
