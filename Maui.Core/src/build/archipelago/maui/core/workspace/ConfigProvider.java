package build.archipelago.maui.core.workspace;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.maui.core.exceptions.PackageNotLocalException;
import build.archipelago.maui.core.workspace.cache.PackageCacher;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContext;
import build.archipelago.maui.core.workspace.models.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Slf4j
public class ConfigProvider {

    private Map<String, BuildConfig> configCache;
    private WorkspaceContext workspace;
    private PackageCacher packageCacher;

    public ConfigProvider(WorkspaceContext workspace, PackageCacher packageCacher) {
        this.workspace = workspace;
        this.packageCacher = packageCacher;
        configCache = new HashMap<>();
    }

    public BuildConfig getConfig(ArchipelagoBuiltPackage pkg) throws IOException, PackageNotLocalException,
            PackageNotFoundException {
        if (configCache.containsKey(pkg.getBuiltPackageName())) {
            return configCache.get(pkg.getBuiltPackageName());
        }

        BuildConfig buildConfig;
        // When dealing with local packages we only care about the name, as that package will overwrite all other
        // versions of that package in the version-set
        if (workspace.getLocalPackages().stream()
                .anyMatch(lPKG -> lPKG.equalsIgnoreCase(pkg.getName()))) {
            buildConfig = provideLocalConfig(pkg);
        } else {
            buildConfig = provideCacheConfig(pkg);
        }
        configCache.put(pkg.getBuiltPackageName(), buildConfig);
        return buildConfig;
    }

    private BuildConfig provideLocalConfig(ArchipelagoPackage pkg) throws PackageNotLocalException, IOException {
        Path root = workspace.getPackageRoot(pkg);
        Path configFile = root.resolve(WorkspaceConstants.BUILD_FILE_NAME);
        if (!Files.exists(configFile)) {
            log.error("Found the package root \"{}\" but it did not contain a config file \"{}\"",
                    root, WorkspaceConstants.BUILD_FILE_NAME);
            throw new PackageNotLocalException(pkg);
        }

        return BuildConfig.from(configFile);
    }

    private BuildConfig provideCacheConfig(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException,
            PackageNotLocalException, IOException {
        Path root = packageCacher.getCachePath(pkg);
        Path configFile = root.resolve(WorkspaceConstants.BUILD_FILE_NAME);
        if (!Files.exists(configFile)) {
            log.error("Found the package root \"{}\" but it did not contain a config file \"{}\"",
                    root, WorkspaceConstants.BUILD_FILE_NAME);
            throw new PackageNotLocalException(pkg);
        }

        return BuildConfig.from(configFile);
    }
}
