package build.archipelago.maui.commands;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.core.workspace.cache.PackageCacher;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContext;
import build.archipelago.maui.core.workspace.models.BuildConfig;
import build.archipelago.maui.utils.WorkspaceUtils;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Slf4j
public abstract class BaseCommand implements Callable<Integer> {
    protected WorkspaceContext ws;
    protected ArchipelagoPackage pkg;
    protected BuildConfig buildConfig;
    protected Path pkgDir;
    protected Path wsDir;

    protected boolean requireWorkspace(VersionServiceClient vsClient, PackageCacher packageCacher) {
        wsDir = WorkspaceUtils.getWorkspaceDir();
        if (wsDir == null) {
            return false;
        }
        ws = new WorkspaceContext(wsDir, vsClient, packageCacher);
        try {
            ws.load();
        } catch (IOException e) {
            log.error("Failed to load workspace context", e);
            return false;
        }
        return true;
    }

    protected boolean requirePackage() {
        if (ws == null) {
            throw new RuntimeException("Workspace is not loaded");
        }

        pkgDir = WorkspaceUtils.getPackageDir(ws.getRoot());
        if (pkgDir == null) {
            return false;
        }

        buildConfig = null;
        try {
            buildConfig = BuildConfig.from(pkgDir);
        } catch (IOException e) {
            log.error("Failed to load build config from package", e);
            return false;
        }

        String version = buildConfig.getVersion();
        String packageName = pkgDir.getFileName().toString();
        pkg = new ArchipelagoPackage(packageName, version);
        return true;
    }
}
