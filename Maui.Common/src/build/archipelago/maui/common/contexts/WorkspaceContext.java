package build.archipelago.maui.common.contexts;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.versionset.*;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.common.WorkspaceConstants;
import build.archipelago.maui.common.cache.PackageCacher;
import build.archipelago.maui.common.models.*;
import build.archipelago.maui.common.serializer.*;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
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
    public Path getPackageRoot(ArchipelagoPackage pkg) throws PackageNotLocalException {
        Path packagePath = root.resolve(pkg.getName());
        if (!Files.exists(packagePath)) {
            log.warn("The requested package \"{}\" is not in the workspaces root \"{}\"",
                    pkg.getNameVersion(), root);
            throw new PackageNotLocalException(pkg);
        }
        return packagePath;
    }
    public Path getPackageBuildPath(ArchipelagoPackage pkg) throws PackageNotLocalException {
        return getPackageRoot(pkg).resolve(WorkspaceConstants.BUILD_DIR);
    }

    public void saveRevisionCache(VersionSetRevision vsRevision) throws IOException {
        Preconditions.checkNotNull(vsRevision);
        VersionSetRevisionSerializer.save(vsRevision, root);
    }

    public VersionSetRevision getVersionSetRevision() throws IOException, VersionSetNotSyncedException {
        if (this.versionSetRevision == null) {
            this.versionSetRevision = VersionSetRevisionSerializer.load(root);
        }
        return this.versionSetRevision;
    }

    public void clearVersionSetRevisionCache() throws IOException {
        VersionSetRevisionSerializer.clear(root);
    }

    public boolean isPackageInVersionSet(ArchipelagoPackage targetPackage) throws IOException, VersionSetNotSyncedException {
        Preconditions.checkNotNull(targetPackage);

        for (ArchipelagoBuiltPackage pkg : getVersionSetRevision().getPackages()) {
            if (pkg.equals(targetPackage)) {
                return true;
            }
        }
        for (ArchipelagoPackage pkg : getLocalArchipelagoPackages()) {
            if (pkg.equals(targetPackage)) {
                return true;
            }
        }
        return false;
    }

    public BuildConfig getConfig(ArchipelagoPackage pkg) throws IOException, PackageNotLocalException,
            PackageNotFoundException, VersionSetNotSyncedException, PackageNotInVersionSetException, LocalPackageMalformedException {
        Preconditions.checkNotNull(pkg);

        BuildConfig buildConfig = configCache.getIfPresent(pkg.getNameVersion());
        if (buildConfig != null) {
            return buildConfig;
        }

        // The Load archipelago packages command will load the configs to get the version number
        if (getLocalArchipelagoPackages().stream().anyMatch(lPKG -> lPKG.equals(pkg))) {
            buildConfig = provideLocalConfig(pkg);
        } else {
            buildConfig = provideCacheConfig(pkg);
        }
        configCache.put(pkg.getNameVersion(), buildConfig);
        return buildConfig;
    }

    private BuildConfig provideLocalConfig(ArchipelagoPackage pkg) throws PackageNotLocalException, IOException, LocalPackageMalformedException {
        Preconditions.checkNotNull(pkg);

        Path root = getPackageRoot(pkg);
        Path configFile = root.resolve(WorkspaceConstants.BUILD_FILE_NAME);
        if (!Files.exists(configFile)) {
            log.error("Found the package root \"{}\" but it did not contain a config file \"{}\"",
                    root, WorkspaceConstants.BUILD_FILE_NAME);
            throw new LocalPackageMalformedException(pkg);
        }

        return BuildConfig.from(root);
    }

    private BuildConfig provideCacheConfig(ArchipelagoPackage pkg) throws PackageNotFoundException,
            PackageNotLocalException, IOException, VersionSetNotSyncedException, PackageNotInVersionSetException {
        Preconditions.checkNotNull(pkg);

        val buildPackage = getVersionSetRevision().getPackages().stream().filter(p -> p.equals(pkg)).findFirst();
        if (buildPackage.isEmpty()) {
            throw new PackageNotInVersionSetException(pkg);
        }
        Path root = packageCacher.getCachePath(buildPackage.get());
        Path configFile = root.resolve(WorkspaceConstants.BUILD_FILE_NAME);
        if (!Files.exists(configFile)) {
            log.error("Found the package root \"{}\" but it did not contain a config file \"{}\"",
                    root, WorkspaceConstants.BUILD_FILE_NAME);
            throw new PackageNotLocalException(pkg);
        }

        return BuildConfig.from(root);
    }

    public List<ArchipelagoPackage> getLocalArchipelagoPackages() {
        if (localArchipelagoPackages == null) {
            localArchipelagoPackages = new ArrayList<>();
            for (String packageName : getLocalPackages()) {
                Path pkgDir = root.resolve(packageName);
                if (!Files.exists(pkgDir) || !Files.isDirectory(pkgDir)) {
                    continue;
                }
                BuildConfig buildConfig;
                try {
                    buildConfig = BuildConfig.from(pkgDir);
                } catch (IOException e) {
                    log.error("Failed to read build config for " + packageName, e);
                    continue;
                }
                localArchipelagoPackages.add(new ArchipelagoPackage(packageName, buildConfig.getVersion()));
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
