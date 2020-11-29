package build.archipelago.buildserver.builder.maui;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.common.PackageSourceProvider;
import build.archipelago.maui.common.cache.*;
import build.archipelago.maui.common.contexts.*;
import build.archipelago.maui.core.actions.*;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.path.MauiPath;
import com.amazonaws.services.dynamodbv2.xspec.M;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
public class Maui {
    private static final String WORKSPACE_NAME = "ws";
    private static final String CACHE_DIR = "cache";
    private static final String TEMP_DIR = "temp";
    private Path buildRoot;
    private Path workspaceLocation;

    private WorkspaceContextFactory workspaceContextFactory;
    private SystemPathProvider systemPathProvider;
    private HarborClient harborClient;
    private PackageCacher packageCacher;
    private PackageSourceProvider packageSourceProvider;
    private MauiPath mauiPath;

    public Maui(Path buildRoot,
                HarborClient harborClient,
                PackageSourceProvider packageSourceProvider,
                MauiPath mauiPath) {
        this.buildRoot = buildRoot;
        this.harborClient = harborClient;
        this.packageSourceProvider = packageSourceProvider;
        this.mauiPath = mauiPath;

        try {
            if (!Files.exists(buildRoot) || !Files.isDirectory(buildRoot)) {
                throw new RuntimeException("The build root " + buildRoot + " dose not exists or is not a directory");
            }
            Path cacheDir = buildRoot.resolve(CACHE_DIR);
            if (!Files.exists(cacheDir)) {
                Files.createDirectory(cacheDir);
            }
            Path tempDir = buildRoot.resolve(TEMP_DIR);
            if (!Files.exists(tempDir)) {
                Files.createDirectory(tempDir);
            }

            packageCacher = new LocalPackageCacher(cacheDir, tempDir, harborClient);
            workspaceContextFactory = new WorkspaceContextFactory(packageCacher);

            workspaceLocation = buildRoot.resolve(WORKSPACE_NAME);
            systemPathProvider = new SystemPathProvider();
            systemPathProvider.overrideCurrentDir(workspaceLocation);
        } catch (IOException exp) {
            throw new RuntimeException(exp);
        }
    }

    public Path getWorkspaceLocation() {
        return workspaceLocation;
    }

    public WorkspaceContext getWorkspaceContext() {
        return workspaceContextFactory.create(getWorkspaceLocation());
    }

    public boolean createWorkspace(OutputWrapper outputWrapper, String versionSet) {
        SystemPathProvider systemPathProvider = new SystemPathProvider();
        systemPathProvider.overrideCurrentDir(buildRoot);

        WorkspaceCreateAction workspaceCreateAction = new WorkspaceCreateAction(
                workspaceContextFactory, systemPathProvider, outputWrapper, harborClient);
        return workspaceCreateAction.createWorkspace(WORKSPACE_NAME, versionSet);
    }

    public boolean syncWorkspace(OutputWrapper outputWrapper, ExecutorService executorService) {
        WorkspaceSyncAction workspaceSyncAction = new WorkspaceSyncAction(
                workspaceContextFactory,
                systemPathProvider,
                outputWrapper,
                harborClient,
                packageCacher,
                executorService);
        return workspaceSyncAction.syncWorkspace(null);
    }

    public boolean usePackage(OutputWrapper outputWrapper, String pkg) {
        WorkspaceUseAction action = new WorkspaceUseAction(workspaceContextFactory,
                systemPathProvider, outputWrapper, harborClient, packageSourceProvider);
        return action.usePackages(List.of(pkg));
    }

    public boolean build(OutputWrapper outputWrapper, ArchipelagoPackage pkg) {
        SystemPathProvider systemPathProvider = new SystemPathProvider();
        systemPathProvider.overrideCurrentDir(getWorkspaceLocation().resolve(pkg.getName()));

        BuildAction action = new BuildAction(workspaceContextFactory, systemPathProvider, outputWrapper, mauiPath);
        try {
            return action.build(List.of("build"));
        } catch (Exception e) {
            log.error("Failed to build " + pkg + " with exception", e);
            throw new RuntimeException(e);
        }
    }
}
