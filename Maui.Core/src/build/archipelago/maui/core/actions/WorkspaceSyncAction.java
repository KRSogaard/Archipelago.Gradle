package build.archipelago.maui.core.actions;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.concurrent.Wrap;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.common.cache.PackageCacheList;
import build.archipelago.maui.common.cache.PackageCacher;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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
        if (!setupWorkspaceContext()) {
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

        if (!syncVersionSet(vs, versionSetRevision)) {
            return false;
        }
        try {
            workspaceContext.saveRevisionCache(versionSetRevision);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
        for(ArchipelagoBuiltPackage pkg : packagesToSync) {
            log.trace("Syncing {} to cache", pkg.toString());
            pkgFutures.add(executor.submit(() -> {
                try {
                    packageCacher.cache(pkg);
                } catch (PackageNotFoundException e) {
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
            return false;
        }
        return true;
    }
}
