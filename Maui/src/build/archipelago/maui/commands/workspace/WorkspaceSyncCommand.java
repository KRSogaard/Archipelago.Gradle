package build.archipelago.maui.commands.workspace;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.concurrent.Wrap;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.versionset.*;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.Output.OutputWrapper;
import build.archipelago.maui.commands.BaseCommand;
import build.archipelago.maui.common.cache.*;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@CommandLine.Command(name = "sync", mixinStandardHelpOptions = true, description = "Synchronize the current workspace the the version-set")
public class WorkspaceSyncCommand extends BaseCommand {

    @CommandLine.ParentCommand
    private WorkspaceCommand parent;

    @CommandLine.Option(names = { "-rev", "--revision"})
    private String revision;

    private PackageCacher packageCacher;
    private HarborClient harborClient;
    private ExecutorService executor;

    public WorkspaceSyncCommand(WorkspaceContextFactory workspaceContextFactory,
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

    @Override
    public Integer call() throws Exception {
        if (!requireWorkspace()) {
            out.error("Was unable to locate the workspace");
            return 1;
        }

        if (workspaceContext.getVersionSet() == null) {
            out.write("No version set is assigned to the workspace, can not sync.");
            return 1;
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
            return 1;
        }

        syncVersionSet(vs, versionSetRevision);
        workspaceContext.saveRevisionCache(versionSetRevision);

        return 0;
    }

    public boolean syncVersionSet(VersionSet vs, VersionSetRevision vsRevision) throws VersionSetDoseNotExistsException {
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
