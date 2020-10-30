package build.archipelago.maui.common.contexts;

import build.archipelago.maui.common.cache.PackageCacher;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;

import java.nio.file.Path;

public class WorkspaceContextFactory {

    private PackageCacher packageCacher;

    public WorkspaceContextFactory(PackageCacher packageCacher) {
        this.packageCacher = packageCacher;
    }

    public WorkspaceContext create(Path wsRoot) {
        return new WorkspaceContext(wsRoot, packageCacher);
    }
}
