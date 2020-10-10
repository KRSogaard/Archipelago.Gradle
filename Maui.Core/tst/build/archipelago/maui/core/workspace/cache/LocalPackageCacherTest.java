package build.archipelago.maui.core.workspace.cache;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.GetPackageBuildResponse;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LocalPackageCacherTest {

    private static final ArchipelagoBuiltPackage PKG_1 = ArchipelagoBuiltPackage.parse("pkg1-1.0#abc");
    private static final ArchipelagoBuiltPackage PKG_2 = ArchipelagoBuiltPackage.parse("pkg2-1.2#ffe");
    private static final ArchipelagoBuiltPackage PKG_3 = ArchipelagoBuiltPackage.parse("pkg3-1.0#tgf");
    private static final ArchipelagoBuiltPackage PKG_4 = ArchipelagoBuiltPackage.parse("pkg4-3.0#ujk");
    private static final ArchipelagoBuiltPackage PKG_5 = ArchipelagoBuiltPackage.parse("pkg5-2.0#jhy");

    @Rule
    public TemporaryFolder folderPkgCache;
    @Rule
    public TemporaryFolder folderTemp;

    private LocalPackageCacher cacher;
    private PackageServiceClient packageClient;
    private Path pkgCacheFolder;
    private Path tempCacheFolder;

    @Before
    public void setUp() throws Exception {
        folderPkgCache = new TemporaryFolder();
        folderPkgCache.create();
        folderTemp = new TemporaryFolder();
        folderTemp.create();
        pkgCacheFolder = folderPkgCache.getRoot().toPath();
        tempCacheFolder = folderTemp.getRoot().toPath();

        packageClient = mock(PackageServiceClient.class);
        cacher = new LocalPackageCacher(pkgCacheFolder, tempCacheFolder, packageClient);
    }

    @After
    public void takeDown() {
        folderPkgCache.delete();
    }

    @Test
    public void testCheck1PackageExists() throws IOException {
        createTestData(List.of(PKG_1));

        PackageCacheList list = cacher.getCurrentCachedPackages();
        assertTrue(list.hasPackage(PKG_1));
    }

    @Test
    public void testCheck1PackageExistsAnd1DoNot() throws IOException {
        createTestData(List.of(PKG_1));

        PackageCacheList list = cacher.getCurrentCachedPackages();
        assertTrue(list.hasPackage(PKG_1));
        assertFalse(list.hasPackage(PKG_2));
    }

    @Test
    public void testCachePackage() throws URISyntaxException, PackageNotFoundException, IOException {
        Path zipFile = Paths.get(getClass().getClassLoader().getResource("testPkg-1.0#0001.zip").toURI());
        Path configFile = Paths.get(getClass().getClassLoader().getResource("testPkg-1.0#0001.ISLAND").toURI());

        when(packageClient.getBuildArtifact(any(), any())).thenReturn(zipFile);
        when(packageClient.getPackageBuild(any())).thenReturn(GetPackageBuildResponse.builder()
                .config(Files.readString(configFile))
                .created(Instant.now())
                .hash("1234")
                .build());
        cacher.cache(PKG_1);

        Path pkgPath = pkgCacheFolder.resolve(PKG_1.getBuiltPackageName());
        Path buildPath = pkgPath.resolve("build");
        assertTrue(Files.exists(pkgPath));
        assertTrue(Files.isDirectory(pkgPath));
        assertTrue(Files.exists(pkgPath.resolve("ISLAND")));
        assertTrue(Files.exists(buildPath.resolve("libs/tesPackage-1.0.jar")));
        assertTrue(Files.exists(buildPath.resolve("media/pirate.jpg")));
        assertTrue(Files.exists(buildPath.resolve("test.txt")));

        assertTrue(cacher.getCurrentCachedPackages().hasPackage(PKG_1));
    }

    private void createTestData(List<ArchipelagoBuiltPackage> packages) throws IOException {
        for (ArchipelagoBuiltPackage pkg : packages) {
            folderPkgCache.newFolder(pkg.getBuiltPackageName());
        }
    }
}