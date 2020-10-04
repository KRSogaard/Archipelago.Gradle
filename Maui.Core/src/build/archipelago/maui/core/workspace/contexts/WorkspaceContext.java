package build.archipelago.maui.core.workspace.contexts;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.versionset.*;
import build.archipelago.maui.core.exceptions.*;
import build.archipelago.maui.core.workspace.WorkspaceConstants;
import build.archipelago.maui.core.workspace.cache.PackageCacher;
import build.archipelago.maui.core.workspace.models.*;
import build.archipelago.maui.core.workspace.serializer.*;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Slf4j
public class WorkspaceContext extends Workspace {

    private VersionServiceClient vsClient;
    private PackageCacher packageCacher;

    @Getter
    private Path root;
    private Map<String, BuildConfig> configCache;
    private VersionSetRevision versionSetRevision;
    private List<ArchipelagoPackage> localArchipelagoPackages;

    public WorkspaceContext(Path root, VersionServiceClient vsClient,
                            PackageCacher packageCacher) {
        super();
        this.root = root;
        this.vsClient = vsClient;
        this.packageCacher = packageCacher;
        configCache = new HashMap<>();
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

    public void create() throws VersionSetDoseNotExistsException, IOException {
        if (versionSet != null) {
            // Verifying that the version-set exists and that we have the correct capitalisation of the version-set
             VersionSet vs = vsClient.getVersionSet(versionSet);
             versionSet = vs.getName();
        }
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
            PackageNotFoundException, VersionSetNotSyncedException, PackageNotInVersionSetException {
        if (configCache.containsKey(pkg.getNameVersion())) {
            return configCache.get(pkg.getNameVersion());
        }

        BuildConfig buildConfig;
        // When dealing with local packages we only care about the name, as that package will overwrite all other
        // versions of that package in the version-set
        if (getLocalPackages().stream()
                .anyMatch(lPKG -> lPKG.equalsIgnoreCase(pkg.getName()))) {
            buildConfig = provideLocalConfig(pkg);
        } else {
            buildConfig = provideCacheConfig(pkg);
        }
        configCache.put(pkg.getNameVersion(), buildConfig);
        return buildConfig;
    }

    private BuildConfig provideLocalConfig(ArchipelagoPackage pkg) throws PackageNotLocalException, IOException {
        Path root = getPackageRoot(pkg);
        Path configFile = root.resolve(WorkspaceConstants.BUILD_FILE_NAME);
        if (!Files.exists(configFile)) {
            log.error("Found the package root \"{}\" but it did not contain a config file \"{}\"",
                    root, WorkspaceConstants.BUILD_FILE_NAME);
            throw new PackageNotLocalException(pkg);
        }

        return BuildConfig.from(root);
    }

    private BuildConfig provideCacheConfig(ArchipelagoPackage pkg) throws PackageNotFoundException,
            PackageNotLocalException, IOException, VersionSetNotSyncedException, PackageNotInVersionSetException {
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

        return BuildConfig.from(configFile);
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
}
