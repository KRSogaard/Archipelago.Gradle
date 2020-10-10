package build.archipelago.maui.core.workspace.contexts;

import build.archipelago.maui.core.workspace.cache.PackageCacher;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;

import java.nio.file.Path;

public class WorkspaceContextFactory {

    private VersionSetServiceClient vsClient;
    private PackageCacher packageCacher;

    public WorkspaceContextFactory(VersionSetServiceClient vsClient,
                                   PackageCacher packageCacher) {
        this.vsClient = vsClient;
        this.packageCacher = packageCacher;
    }

    public WorkspaceContext create(Path wsRoot) {
        return new WorkspaceContext(wsRoot, vsClient, packageCacher);
    }
}
