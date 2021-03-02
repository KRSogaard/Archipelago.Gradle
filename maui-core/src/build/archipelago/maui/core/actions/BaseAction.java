package build.archipelago.maui.core.actions;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.PackageNotLocalException;
import build.archipelago.maui.common.contexts.*;
import build.archipelago.maui.common.models.BuildConfig;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public abstract class BaseAction {
    protected WorkspaceContext workspaceContext;
    protected ArchipelagoPackage commandPKG;
    protected BuildConfig buildConfig;
    protected Path pkgDir;
    protected Path wsDir;

    protected WorkspaceContextFactory workspaceContextFactory;
    protected SystemPathProvider systemPathProvider;
    protected OutputWrapper out;

    public BaseAction(WorkspaceContextFactory workspaceContextFactory,
                       SystemPathProvider systemPathProvider,
                       OutputWrapper out) {
        this.workspaceContextFactory = workspaceContextFactory;
        this.systemPathProvider = systemPathProvider;
        this.out = out;
    }

    protected boolean setupWorkspaceContext() {
        wsDir = systemPathProvider.getWorkspaceDir();
        if (wsDir == null) {
            return false;
        }
        workspaceContext = workspaceContextFactory.create(wsDir);
        try {
            workspaceContext.load();
        } catch (IOException e) {
            log.error("Failed to load workspace context", e);
            return false;
        }
        return true;
    }

    protected boolean setupPackage() {
        if (workspaceContext == null) {
            throw new RuntimeException("Workspace is not loaded");
        }

        pkgDir = systemPathProvider.getPackageDir(workspaceContext.getRoot());
        if (pkgDir == null) {
            return false;
        }

        buildConfig = null;
        try {
            buildConfig = BuildConfig.from(pkgDir);
        } catch (PackageNotLocalException e) {
            log.error("Failed to load build config from package", e);
            return false;
        }

        String version = buildConfig.getVersion();
        String packageName = pkgDir.getFileName().toString();
        commandPKG = new ArchipelagoPackage(packageName, version);
        return true;
    }
}
