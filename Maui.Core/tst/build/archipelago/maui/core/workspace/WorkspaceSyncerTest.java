package build.archipelago.maui.core.workspace;


import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.concurrent.BlockingExecutorServiceFactory;
import build.archipelago.common.concurrent.ExecutorServiceFactory;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.maui.core.TestData;
import build.archipelago.maui.core.workspace.cache.PackageCacheList;
import build.archipelago.maui.core.workspace.cache.PackageCacher;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class WorkspaceSyncerTest {

    private static final VersionSet VERSION_SET = VersionSet.builder()
            .name("TestVersionSet/master")
            .parent("live")
            .latestRevision("ews")
            .latestRevisionCreated(Instant.now())
            .created(Instant.now())
            .build();

    private WorkspaceSyncer syncer;
    private VersionServiceClient vsClient;
    private PackageServiceClient packageClient;
    private ExecutorService executor;
    private PackageCacheList packageCacheList;
    private PackageCacher packageCacher;


    @Before
    public void setUp() {
        BlockingExecutorServiceFactory executorServiceFactory = new BlockingExecutorServiceFactory();
        executorServiceFactory.setQueueCapacity(2);
        executorServiceFactory.setMaximumPoolSize(1);

        vsClient = mock(VersionServiceClient.class);
        packageCacher = mock(PackageCacher.class);
        packageCacheList = mock(PackageCacheList.class);

        when(packageCacher.getCurrentCachedPackages()).thenReturn(packageCacheList);

        syncer = new WorkspaceSyncer(packageCacher, vsClient, executorServiceFactory);
    }

    @Test
    public void testClearCache5NewPackages() throws VersionSetDoseNotExistsException, PackageNotFoundException, IOException {
        when(vsClient.getVersionSet(any())).thenReturn(VERSION_SET);
        when(vsClient.getVersionSetPackages(any(), any())).thenReturn(VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1,TestData.PKG_2,TestData.PKG_3,TestData.PKG_4,TestData.PKG_5))
                .build());
        when(packageCacheList.hasPackage(any())).thenReturn(false);

        assertTrue(syncer.syncVersionSet(VERSION_SET.getName().toLowerCase()));
        verify(packageCacher, times(1)).cache(eq(TestData.PKG_1));
        verify(packageCacher, times(1)).cache(eq(TestData.PKG_2));
        verify(packageCacher, times(1)).cache(eq(TestData.PKG_3));
        verify(packageCacher, times(1)).cache(eq(TestData.PKG_4));
        verify(packageCacher, times(1)).cache(eq(TestData.PKG_5));
    }

    @Test
    public void test2CachedPackagesCache3NewPackages() throws VersionSetDoseNotExistsException, PackageNotFoundException, IOException {
        when(vsClient.getVersionSet(any())).thenReturn(VERSION_SET);
        when(vsClient.getVersionSetPackages(any(), any())).thenReturn(VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1,TestData.PKG_2,TestData.PKG_3,TestData.PKG_4,TestData.PKG_5))
                .build());
        when(packageCacheList.hasPackage(eq(TestData.PKG_1))).thenReturn(true);
        when(packageCacheList.hasPackage(eq(TestData.PKG_2))).thenReturn(true);
        when(packageCacheList.hasPackage(eq(TestData.PKG_3))).thenReturn(false);
        when(packageCacheList.hasPackage(eq(TestData.PKG_4))).thenReturn(false);
        when(packageCacheList.hasPackage(eq(TestData.PKG_5))).thenReturn(false);

        assertTrue(syncer.syncVersionSet(VERSION_SET.getName().toLowerCase()));
        verify(packageCacher, never()).cache(eq(TestData.PKG_1));
        verify(packageCacher, never()).cache(eq(TestData.PKG_2));
        verify(packageCacher, times(1)).cache(eq(TestData.PKG_3));
        verify(packageCacher, times(1)).cache(eq(TestData.PKG_4));
        verify(packageCacher, times(1)).cache(eq(TestData.PKG_5));
    }

    @Test
    public void test2CachedPackagesCache0NewPackages() throws VersionSetDoseNotExistsException, PackageNotFoundException, IOException {
        when(vsClient.getVersionSet(any())).thenReturn(VERSION_SET);
        when(vsClient.getVersionSetPackages(any(), any())).thenReturn(VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1,TestData.PKG_2))
                .build());
        when(packageCacheList.hasPackage(eq(TestData.PKG_1))).thenReturn(true);
        when(packageCacheList.hasPackage(eq(TestData.PKG_2))).thenReturn(true);

        assertTrue(syncer.syncVersionSet(VERSION_SET.getName().toLowerCase()));
        verify(packageCacher, never()).cache(eq(TestData.PKG_1));
        verify(packageCacher, never()).cache(eq(TestData.PKG_2));
    }


    @Test
    public void testReturnsFalseWithException() throws VersionSetDoseNotExistsException {
        when(vsClient.getVersionSet(any())).thenReturn(VERSION_SET);
        when(vsClient.getVersionSetPackages(any(), any())).thenReturn(VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1,TestData.PKG_2))
                .build());
        when(packageCacheList.hasPackage(eq(TestData.PKG_1))).thenThrow(new RuntimeException("Test"));
        assertFalse(syncer.syncVersionSet(VERSION_SET.getName().toLowerCase()));
    }

}