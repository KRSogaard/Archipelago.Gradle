package build.archipelago.maui.common.contexts;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.maui.common.WorkspaceConstants;
import build.archipelago.maui.common.cache.PackageCacher;
import build.archipelago.maui.common.models.*;
import build.archipelago.maui.common.serializer.*;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import com.github.benmanes.caffeine.cache.*;
import com.google.common.base.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Slf4j
public class WorkspaceContext extends Workspace {

    private PackageCacher packageCacher;

    @Getter
    private Path root;
    private Cache<String, BuildConfig> configCache;
    private VersionSetRevision versionSetRevision;
    private List<ArchipelagoPackage> localArchipelagoPackages;

    public WorkspaceContext(Path root,
                            PackageCacher packageCacher) {
        super();
        this.root = root;
        this.packageCacher = packageCacher;

        configCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .build();
    }

    public void load() throws IOException {
        Path workspaceFile = getWorkspaceFile(root);
        if (!Files.exists(workspaceFile)) {
            throw new FileNotFoundException(workspaceFile.toString());
        }

        Workspace ws = WorkspaceSerializer.load(root);
        this.setVersionSet(ws.getVersionSet());
        this.setLocalPackages(ws.getLocalPackages());
    }

    public void save() throws IOException {
        WorkspaceSerializer.save(this, root);
    }

    public static Path getWorkspaceFile(Path path) {
        return path.resolve(WorkspaceConstants.WORKSPACE_FILE_NAME);
    }

    // TODO: Change this to be aware of the cached packages
    public Path getPackageRoot(ArchipelagoPackage pkg) throws PackageNotFoundException {

        if (this.getLocalArchipelagoPackages().stream().anyMatch(lPKG -> lPKG.equals(pkg))) {
            log.debug("The package '{}' is local", pkg.getName());
            Path packagePath = root.resolve(pkg.getName());
            if (!Files.exists(packagePath)) {
                log.debug("The package '{}' was not found in '{}' was it removed by the user?", pkg.getName(), root);
                log.warn("The requested package '{}' is not in the workspaces root '{}'",
                        pkg.getNameVersion(), root);
                throw new PackageNotFoundException(pkg);
            }
            return packagePath;
        } else {
            log.debug("The package '{}' is not local, checking the cache", pkg.getName());
            try {
                val buildPackage = this.getVersionSetRevision().getPackages().stream().filter(p -> p.equals(pkg)).findFirst();
                if (buildPackage.isEmpty()) {
                    log.debug("The package '{}' was not in the version set '{}'", pkg.getNameVersion(), this.getVersionSet());
                    throw new PackageNotFoundException(pkg);
                }
                return packageCacher.getCachePath(buildPackage.get());
            } catch (VersionSetNotSyncedException exp) {
                log.error("The version set has not been synced");
                throw new PackageNotFoundException(pkg);
            }
        }
    }

    public Path getPackageBuildPath(ArchipelagoPackage pkg) throws PackageNotFoundException {
        return this.getPackageRoot(pkg).resolve(WorkspaceConstants.BUILD_DIR);
    }

    public void saveRevisionCache(VersionSetRevision vsRevision) {
        Preconditions.checkNotNull(vsRevision);
        VersionSetRevisionSerializer.save(vsRevision, root);
    }

    public VersionSetRevision getVersionSetRevision() throws VersionSetNotSyncedException {
        if (this.versionSetRevision == null) {
            this.versionSetRevision = VersionSetRevisionSerializer.load(root);
        }
        return this.versionSetRevision;
    }

    public void clearVersionSetRevisionCache() throws IOException {
        VersionSetRevisionSerializer.clear(root);
    }

    public boolean isPackageInVersionSet(ArchipelagoPackage targetPackage) throws VersionSetNotSyncedException {
        Preconditions.checkNotNull(targetPackage);

        for (ArchipelagoBuiltPackage pkg : this.getVersionSetRevision().getPackages()) {
            if (pkg.equals(targetPackage)) {
                return true;
            }
        }
        for (ArchipelagoPackage pkg : this.getLocalArchipelagoPackages()) {
            if (pkg.equals(targetPackage)) {
                return true;
            }
        }
        return false;
    }

    public BuildConfig getConfig(ArchipelagoPackage pkg) throws PackageNotLocalException,
            PackageNotFoundException, VersionSetNotSyncedException, PackageNotInVersionSetException, LocalPackageMalformedException {
        Preconditions.checkNotNull(pkg);

        BuildConfig buildConfig = configCache.getIfPresent(pkg.getNameVersion());
        if (buildConfig != null) {
            return buildConfig;
        }


        Path root = this.getPackageRoot(pkg);
        Path configFile = root.resolve(WorkspaceConstants.BUILD_FILE_NAME);
        if (!Files.exists(configFile)) {
            log.error("Found the package root '{}' but it did not contain a config file '{}'",
                    root, WorkspaceConstants.BUILD_FILE_NAME);
            throw new LocalPackageMalformedException(pkg);
        }

        buildConfig = BuildConfig.from(root);
        configCache.put(pkg.getNameVersion(), buildConfig);
        return buildConfig;
    }

    public List<ArchipelagoPackage> getLocalArchipelagoPackages() {
        if (localArchipelagoPackages == null) {
            localArchipelagoPackages = new ArrayList<>();
            for (String packageName : this.getLocalPackages()) {
                Path pkgDir = root.resolve(packageName);
                if (!Files.exists(pkgDir) || !Files.isDirectory(pkgDir)) {
                    continue;
                }
                try {
                    BuildConfig buildConfig = BuildConfig.from(pkgDir);
                    localArchipelagoPackages.add(new ArchipelagoPackage(packageName, buildConfig.getVersion()));
                } catch (PackageNotLocalException e) {
                    log.error(String.format("Got a package not local exception when getting local packages for '%s'", packageName), e);
                    throw new RuntimeException(e);
                }
            }
        }
        return localArchipelagoPackages;
    }

    public void addLocalPackage(String packageName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(packageName));

        if (localPackages.stream().anyMatch(lp -> lp.equalsIgnoreCase(packageName))) {
            return;
        }
        localPackages.add(packageName);
    }

    public void removeLocalPackage(String packageName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(packageName));

        if (localPackages.stream().noneMatch(lp -> lp.equalsIgnoreCase(packageName))) {
            return;
        }
        localPackages.remove(packageName);
    }
}
