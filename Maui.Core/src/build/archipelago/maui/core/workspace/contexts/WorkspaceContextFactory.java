package build.archipelago.maui.core.workspace.contexts;

import build.archipelago.maui.core.workspace.cache.PackageCacher;
import build.archipelago.versionsetservice.client.VersionServiceClient;

import java.nio.file.Path;

public class WorkspaceContextFactory {

    private VersionServiceClient vsClient;
    private PackageCacher packageCacher;

    public WorkspaceContextFactory(VersionServiceClient vsClient,
                                   PackageCacher packageCacher) {
        this.vsClient = vsClient;
        this.packageCacher = packageCacher;
    }

    public WorkspaceContext create(Path wsRoot) {
        return new WorkspaceContext(wsRoot, vsClient, packageCacher);
    }
}
