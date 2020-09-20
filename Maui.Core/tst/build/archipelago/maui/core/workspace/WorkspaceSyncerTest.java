package build.archipelago.maui.core.workspace;


import build.archipelago.common.concurrent.BlockingExecutorServiceFactory;
import build.archipelago.common.concurrent.ExecutorServiceFactory;
import build.archipelago.maui.core.workspace.cache.PackageCacher;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import org.junit.Before;
import static org.mockito.Mockito.mock;

import java.util.concurrent.ExecutorService;

public class WorkspaceSyncerTest {

    private WorkspaceSyncer syncer;
    private VersionServiceClient vsClient;
    private PackageServiceClient packageClient;
    private ExecutorService executor;
    private PackageCacher packageCacher;


    @Before
    public void setUp() {
        BlockingExecutorServiceFactory executorServiceFactory = new BlockingExecutorServiceFactory();
        executorServiceFactory.setQueueCapacity(5);
        executorServiceFactory.setMaximumPoolSize(2);

        vsClient = mock(VersionServiceClient.class);
        packageCacher = mock(PackageCacher.class);
        syncer = new WorkspaceSyncer(packageCacher, vsClient, executorServiceFactory);
    }

}