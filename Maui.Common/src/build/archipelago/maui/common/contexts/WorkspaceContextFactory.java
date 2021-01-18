package build.archipelago.maui.common.contexts;

import build.archipelago.maui.common.cache.PackageCacher;
import com.github.benmanes.caffeine.cache.*;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class WorkspaceContextFactory {

    private PackageCacher packageCacher;
    private Cache<Path, WorkspaceContext> contextCache;

    public WorkspaceContextFactory(PackageCacher packageCacher) {
        this.packageCacher = packageCacher;
        contextCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }

    public WorkspaceContext create(Path wsRoot) {
        return contextCache.get(wsRoot, p -> new WorkspaceContext(wsRoot, packageCacher));
    }
}
