package build.archipelago.maui.core.workspace.path.graph;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.maui.core.TestData;
import build.archipelago.maui.core.exceptions.*;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContext;
import build.archipelago.maui.core.workspace.models.BuildConfig;
import build.archipelago.maui.core.workspace.path.DependencyTransversalType;
import org.junit.*;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DependencyGraphGeneratorTest {

    private static final BuildConfig EMPTY_BUILD_CONFIG = BuildConfig.builder()
            .buildSystem("Copy")
            .build();

    private WorkspaceContext workspaceContext;

    @Before
    public void setUp() throws Exception {
        workspaceContext = mock(WorkspaceContext.class);
        DependencyGraphGenerator.clearCache();
    }

    @Test
    public void testRootPackageWithNoDependencies() throws PackageNotLocalException, IOException,
            PackageNotFoundException, PackageNotInVersionSetException, PackageDependencyLoopDetectedException, PackageVersionConflictException, VersionSetNotSyncedException, LocalPackageMalformedException {
        VersionSetRevision versionSetRevision = VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1))
                .build();

        when(workspaceContext.getConfig(any())).thenReturn(EMPTY_BUILD_CONFIG);
        when(workspaceContext.getVersionSetRevision()).thenReturn(versionSetRevision);

        ArchipelagoDependencyGraph graph = DependencyGraphGenerator.generateGraph(workspaceContext, TestData.PKG_1, DependencyTransversalType.ALL);

        assertNotNull(graph);
        assertEquals(1, graph.vertexSet().size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_1).size());
    }

    @Test
    public void testRootPackageWith2DependenciesNoTransitive() throws PackageNotLocalException, IOException,
            PackageNotFoundException, PackageNotInVersionSetException, PackageDependencyLoopDetectedException, PackageVersionConflictException, VersionSetNotSyncedException, LocalPackageMalformedException {
        VersionSetRevision versionSetRevision = VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1, TestData.PKG_2, TestData.PKG_3))
                .build();

        BuildConfig buildConfig = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_2, TestData.PKG_3))
                .build();

        when(workspaceContext.getConfig(any())).thenReturn(EMPTY_BUILD_CONFIG);
        when(workspaceContext.getConfig(eq(TestData.PKG_1))).thenReturn(buildConfig);
        when(workspaceContext.getVersionSetRevision()).thenReturn(versionSetRevision);

        ArchipelagoDependencyGraph graph = DependencyGraphGenerator.generateGraph(workspaceContext, TestData.PKG_1, DependencyTransversalType.ALL);

        assertNotNull(graph);
        assertEquals(3, graph.vertexSet().size());
        assertEquals(2, graph.outgoingEdgesOf(TestData.PKG_1).size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_2).size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_3).size());
    }

    @Test
    public void testRootPackageWith2DependenciesDependencyHaveShareDependencyWithRoot() throws PackageNotLocalException, IOException,
            PackageNotFoundException, PackageNotInVersionSetException, PackageDependencyLoopDetectedException, PackageVersionConflictException, VersionSetNotSyncedException, LocalPackageMalformedException {
        VersionSetRevision versionSetRevision = VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1, TestData.PKG_2, TestData.PKG_3))
                .build();

        BuildConfig buildConfig = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_2, TestData.PKG_3))
                .build();

        BuildConfig buildConfig2 = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_3))
                .build();

        when(workspaceContext.getConfig(any())).thenReturn(EMPTY_BUILD_CONFIG);
        when(workspaceContext.getConfig(eq(TestData.PKG_1))).thenReturn(buildConfig);
        when(workspaceContext.getConfig(eq(TestData.PKG_2))).thenReturn(buildConfig2);
        when(workspaceContext.getVersionSetRevision()).thenReturn(versionSetRevision);

        ArchipelagoDependencyGraph graph = DependencyGraphGenerator.generateGraph(workspaceContext, TestData.PKG_1, DependencyTransversalType.ALL);

        assertNotNull(graph);
        assertEquals(3, graph.vertexSet().size());
        assertEquals(2, graph.outgoingEdgesOf(TestData.PKG_1).size());
        assertEquals(1, graph.outgoingEdgesOf(TestData.PKG_2).size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_3).size());
    }

    @Test
    public void testRootDependenciesSharingSameDependency() throws PackageNotLocalException, IOException,
            PackageNotFoundException, PackageNotInVersionSetException, PackageDependencyLoopDetectedException, PackageVersionConflictException, VersionSetNotSyncedException, LocalPackageMalformedException {
        VersionSetRevision versionSetRevision = VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1, TestData.PKG_2, TestData.PKG_3, TestData.PKG_4))
                .build();

        BuildConfig buildConfig = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_2, TestData.PKG_3))
                .build();

        BuildConfig buildConfig2 = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_4))
                .build();

        when(workspaceContext.getConfig(any())).thenReturn(EMPTY_BUILD_CONFIG);
        when(workspaceContext.getConfig(eq(TestData.PKG_1))).thenReturn(buildConfig);
        when(workspaceContext.getConfig(eq(TestData.PKG_2))).thenReturn(buildConfig2);
        when(workspaceContext.getConfig(eq(TestData.PKG_3))).thenReturn(buildConfig2);
        when(workspaceContext.getVersionSetRevision()).thenReturn(versionSetRevision);

        ArchipelagoDependencyGraph graph = DependencyGraphGenerator.generateGraph(workspaceContext, TestData.PKG_1, DependencyTransversalType.ALL);

        assertNotNull(graph);
        assertEquals(4, graph.vertexSet().size());
        assertEquals(2, graph.outgoingEdgesOf(TestData.PKG_1).size());
        assertEquals(1, graph.outgoingEdgesOf(TestData.PKG_2).size());
        assertEquals(1, graph.outgoingEdgesOf(TestData.PKG_3).size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_4).size());
    }

    @Test
    public void testGetTestDependencies() throws PackageNotLocalException, PackageNotInVersionSetException, PackageNotFoundException, PackageDependencyLoopDetectedException, IOException, PackageVersionConflictException, VersionSetNotSyncedException, LocalPackageMalformedException {
        VersionSetRevision versionSetRevision = VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1, TestData.PKG_2, TestData.PKG_3, TestData.PKG_4, TestData.PKG_5))
                .build();

        BuildConfig buildConfig = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_2, TestData.PKG_3))
                .test(List.of(TestData.PKG_4))
                .buildTools(List.of(TestData.PKG_5))
                .build();

        when(workspaceContext.getConfig(any())).thenReturn(EMPTY_BUILD_CONFIG);
        when(workspaceContext.getConfig(eq(TestData.PKG_1))).thenReturn(buildConfig);
        when(workspaceContext.getVersionSetRevision()).thenReturn(versionSetRevision);

        ArchipelagoDependencyGraph graph = DependencyGraphGenerator.generateGraph(workspaceContext, TestData.PKG_1, DependencyTransversalType.TEST);

        assertNotNull(graph);
        assertEquals(4, graph.vertexSet().size());
        assertEquals(3, graph.outgoingEdgesOf(TestData.PKG_1).size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_2).size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_3).size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_4).size());
    }

    @Test
    public void testGetBuildDependencies() throws PackageNotLocalException, PackageNotInVersionSetException, PackageNotFoundException, PackageDependencyLoopDetectedException, IOException, PackageVersionConflictException, VersionSetNotSyncedException, LocalPackageMalformedException {
        VersionSetRevision versionSetRevision = VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1, TestData.PKG_2, TestData.PKG_3, TestData.PKG_4, TestData.PKG_5))
                .build();

        BuildConfig buildConfig = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_2, TestData.PKG_3))
                .test(List.of(TestData.PKG_4))
                .buildTools(List.of(TestData.PKG_5))
                .build();

        when(workspaceContext.getConfig(any())).thenReturn(EMPTY_BUILD_CONFIG);
        when(workspaceContext.getConfig(eq(TestData.PKG_1))).thenReturn(buildConfig);
        when(workspaceContext.getVersionSetRevision()).thenReturn(versionSetRevision);

        ArchipelagoDependencyGraph graph = DependencyGraphGenerator.generateGraph(workspaceContext, TestData.PKG_1, DependencyTransversalType.BUILD_TOOLS);

        assertNotNull(graph);
        assertEquals(4, graph.vertexSet().size());
        assertEquals(3, graph.outgoingEdgesOf(TestData.PKG_1).size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_2).size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_3).size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_5).size());
    }

    @Test
    public void testGetRuntimeDependenciesWithNoDirectRuntimeDependencies() throws PackageNotLocalException, PackageNotInVersionSetException, PackageNotFoundException, PackageDependencyLoopDetectedException, IOException, PackageVersionConflictException, VersionSetNotSyncedException, LocalPackageMalformedException {
        VersionSetRevision versionSetRevision = VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1, TestData.PKG_2, TestData.PKG_3, TestData.PKG_4, TestData.PKG_5))
                .build();

        BuildConfig buildConfig = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_2, TestData.PKG_3))
                .test(List.of(TestData.PKG_4))
                .buildTools(List.of(TestData.PKG_5))
                .build();

        when(workspaceContext.getConfig(any())).thenReturn(EMPTY_BUILD_CONFIG);
        when(workspaceContext.getConfig(eq(TestData.PKG_1))).thenReturn(buildConfig);
        when(workspaceContext.getVersionSetRevision()).thenReturn(versionSetRevision);

        ArchipelagoDependencyGraph graph = DependencyGraphGenerator.generateGraph(workspaceContext, TestData.PKG_1, DependencyTransversalType.RUNTIME);

        assertNotNull(graph);
        assertEquals(3, graph.vertexSet().size());
        assertEquals(2, graph.outgoingEdgesOf(TestData.PKG_1).size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_2).size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_3).size());
    }

    @Test
    public void testGetRuntimeDependenciesWithDirectRuntimeDependencies() throws PackageNotLocalException, PackageNotInVersionSetException, PackageNotFoundException, PackageDependencyLoopDetectedException, IOException, PackageVersionConflictException, VersionSetNotSyncedException, LocalPackageMalformedException {
        VersionSetRevision versionSetRevision = VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1, TestData.PKG_2, TestData.PKG_3, TestData.PKG_4, TestData.PKG_5))
                .build();

        BuildConfig buildConfig = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_2, TestData.PKG_3))
                .test(List.of(TestData.PKG_4))
                .runtime(List.of(TestData.PKG_5))
                .build();

        when(workspaceContext.getConfig(any())).thenReturn(EMPTY_BUILD_CONFIG);
        when(workspaceContext.getConfig(eq(TestData.PKG_1))).thenReturn(buildConfig);
        when(workspaceContext.getVersionSetRevision()).thenReturn(versionSetRevision);

        ArchipelagoDependencyGraph graph = DependencyGraphGenerator.generateGraph(workspaceContext, TestData.PKG_1, DependencyTransversalType.RUNTIME);

        assertNotNull(graph);
        assertEquals(4, graph.vertexSet().size());
        assertEquals(3, graph.outgoingEdgesOf(TestData.PKG_1).size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_2).size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_3).size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_5).size());
    }

    @Test
    public void testGetTestDependenciesShouldGetDependenciesRuntimeDependencies() throws PackageNotLocalException,
            PackageNotInVersionSetException, PackageNotFoundException, PackageDependencyLoopDetectedException, IOException, PackageVersionConflictException, VersionSetNotSyncedException, LocalPackageMalformedException {
        VersionSetRevision versionSetRevision = VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1, TestData.PKG_2, TestData.PKG_3, TestData.PKG_4, TestData.PKG_5))
                .build();

        BuildConfig buildConfig = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_2))
                .buildTools(List.of(TestData.PKG_3))
                .build();

        BuildConfig buildConfig2 = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_5))
                .buildTools(List.of(TestData.PKG_4))
                .build();

        when(workspaceContext.getConfig(any())).thenReturn(EMPTY_BUILD_CONFIG);
        when(workspaceContext.getConfig(eq(TestData.PKG_1))).thenReturn(buildConfig);
        when(workspaceContext.getConfig(eq(TestData.PKG_3))).thenReturn(buildConfig2);
        when(workspaceContext.getVersionSetRevision()).thenReturn(versionSetRevision);

        ArchipelagoDependencyGraph graph = DependencyGraphGenerator.generateGraph(workspaceContext, TestData.PKG_1, DependencyTransversalType.BUILD_TOOLS);

        assertNotNull(graph);
        assertEquals(4, graph.vertexSet().size());
        assertEquals(2, graph.outgoingEdgesOf(TestData.PKG_1).size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_2).size());
        assertEquals(1, graph.outgoingEdgesOf(TestData.PKG_3).size());
        // We should not see the build tool of the dependency package
        assertEquals(TestData.PKG_5,
                graph.outgoingEdgesOf(TestData.PKG_3).stream().collect(Collectors.toList()).get(0).getDependency().getPackage());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_5).size());
    }

    @Test(expected = PackageDependencyLoopDetectedException.class)
    public void testPackageLoopDetection() throws PackageNotLocalException, IOException,
            PackageNotFoundException, PackageNotInVersionSetException, PackageDependencyLoopDetectedException, PackageVersionConflictException, VersionSetNotSyncedException, LocalPackageMalformedException {
        VersionSetRevision versionSetRevision = VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1, TestData.PKG_2, TestData.PKG_3, TestData.PKG_4))
                .build();

        BuildConfig buildConfig = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_2, TestData.PKG_3))
                .build();

        BuildConfig buildConfig2 = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_1, TestData.PKG_4))
                .build();

        when(workspaceContext.getConfig(eq(TestData.PKG_1))).thenReturn(buildConfig);
        when(workspaceContext.getConfig(eq(TestData.PKG_2))).thenReturn(EMPTY_BUILD_CONFIG);
        when(workspaceContext.getConfig(eq(TestData.PKG_3))).thenReturn(buildConfig2);
        when(workspaceContext.getConfig(eq(TestData.PKG_4))).thenReturn(EMPTY_BUILD_CONFIG);
        when(workspaceContext.getVersionSetRevision()).thenReturn(versionSetRevision);

        DependencyGraphGenerator.generateGraph(workspaceContext, TestData.PKG_1, DependencyTransversalType.ALL);
    }

    @Test(expected = PackageDependencyLoopDetectedException.class)
    public void testDeepPackageLoopDetection() throws PackageNotLocalException, IOException,
            PackageNotFoundException, PackageNotInVersionSetException, PackageDependencyLoopDetectedException, PackageVersionConflictException, VersionSetNotSyncedException, LocalPackageMalformedException {
        VersionSetRevision versionSetRevision = VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1, TestData.PKG_2, TestData.PKG_3, TestData.PKG_4, TestData.PKG_5))
                .build();

        BuildConfig buildConfig = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_2))
                .build();

        BuildConfig buildConfig2 = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_3))
                .build();

        BuildConfig buildConfig3 = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_4))
                .build();

        BuildConfig buildConfig4 = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_5))
                .build();

        BuildConfig buildConfig5 = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_2))
                .build();

        when(workspaceContext.getConfig(eq(TestData.PKG_1))).thenReturn(buildConfig);
        when(workspaceContext.getConfig(eq(TestData.PKG_2))).thenReturn(buildConfig2);
        when(workspaceContext.getConfig(eq(TestData.PKG_3))).thenReturn(buildConfig3);
        when(workspaceContext.getConfig(eq(TestData.PKG_4))).thenReturn(buildConfig4);
        when(workspaceContext.getConfig(eq(TestData.PKG_5))).thenReturn(buildConfig5);
        when(workspaceContext.getVersionSetRevision()).thenReturn(versionSetRevision);

        DependencyGraphGenerator.generateGraph(workspaceContext, TestData.PKG_1, DependencyTransversalType.ALL);
    }

    @Test(expected = PackageNotInVersionSetException.class)
    public void testDependencyMissingFromVersionSet() throws PackageNotLocalException, IOException, PackageNotFoundException, PackageDependencyLoopDetectedException, PackageNotInVersionSetException, PackageVersionConflictException, VersionSetNotSyncedException, LocalPackageMalformedException {
        VersionSetRevision versionSetRevision = VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1))
                .build();

        BuildConfig buildConfig = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_2))
                .build();

        when(workspaceContext.getConfig(any())).thenReturn(EMPTY_BUILD_CONFIG);
        when(workspaceContext.getConfig(eq(TestData.PKG_1))).thenReturn(buildConfig);
        when(workspaceContext.getVersionSetRevision()).thenReturn(versionSetRevision);

        DependencyGraphGenerator.generateGraph(workspaceContext, TestData.PKG_1, DependencyTransversalType.ALL);
    }

    @Test
    public void testRemovePackage() throws PackageNotLocalException, IOException, PackageNotFoundException, PackageDependencyLoopDetectedException, PackageNotInVersionSetException, PackageVersionConflictException, VersionSetNotSyncedException, LocalPackageMalformedException {
        VersionSetRevision versionSetRevision = VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1, TestData.PKG_2, TestData.PKG_3, TestData.PKG_4))
                .build();

        BuildConfig buildConfig = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_2))
                .removeDependencies(List.of(TestData.PKG_4))
                .build();

        BuildConfig buildConfig2 = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_3))
                .build();

        BuildConfig buildConfig3 = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_4))
                .build();

        when(workspaceContext.getConfig(any())).thenReturn(EMPTY_BUILD_CONFIG);
        when(workspaceContext.getConfig(eq(TestData.PKG_1))).thenReturn(buildConfig);
        when(workspaceContext.getConfig(eq(TestData.PKG_2))).thenReturn(buildConfig2);
        when(workspaceContext.getConfig(eq(TestData.PKG_3))).thenReturn(buildConfig3);
        when(workspaceContext.getVersionSetRevision()).thenReturn(versionSetRevision);

        ArchipelagoDependencyGraph graph = DependencyGraphGenerator.generateGraph(workspaceContext, TestData.PKG_1, DependencyTransversalType.BUILD_TOOLS);

        assertNotNull(graph);
        assertEquals(3, graph.vertexSet().size());
        assertTrue(graph.vertexSet().stream().noneMatch(pkg -> pkg.equals(TestData.PKG_4)));
    }

    @Test(expected = PackageVersionConflictException.class)
    public void testVersionConflictDetectionInDependencies() throws PackageNotLocalException, IOException, PackageNotFoundException, PackageDependencyLoopDetectedException, PackageNotInVersionSetException, PackageVersionConflictException, VersionSetNotSyncedException, LocalPackageMalformedException {

        ArchipelagoBuiltPackage VCPKG_1 = ArchipelagoBuiltPackage.parse("vcPKG1-1.0#123");
        ArchipelagoBuiltPackage VCPKG_2 = ArchipelagoBuiltPackage.parse("vcPKG1-2.0#123");

        VersionSetRevision versionSetRevision = VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1, TestData.PKG_2, VCPKG_1, VCPKG_2))
                .build();

        BuildConfig buildConfig = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_2, VCPKG_1))
                .build();

        BuildConfig buildConfig2 = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(VCPKG_2))
                .build();

        when(workspaceContext.getConfig(any())).thenReturn(EMPTY_BUILD_CONFIG);
        when(workspaceContext.getConfig(eq(TestData.PKG_1))).thenReturn(buildConfig);
        when(workspaceContext.getConfig(eq(TestData.PKG_2))).thenReturn(buildConfig2);
        when(workspaceContext.getVersionSetRevision()).thenReturn(versionSetRevision);

        DependencyGraphGenerator.generateGraph(workspaceContext, TestData.PKG_1, DependencyTransversalType.BUILD_TOOLS);
    }

    @Test
    public void testConflictResolveResolvesConflict() throws PackageNotLocalException, IOException, PackageNotFoundException, PackageDependencyLoopDetectedException, PackageVersionConflictException, PackageNotInVersionSetException, VersionSetNotSyncedException, LocalPackageMalformedException {
        ArchipelagoBuiltPackage VCPKG_1 = ArchipelagoBuiltPackage.parse("vcPKG1-1.0#123");
        ArchipelagoBuiltPackage VCPKG_2 = ArchipelagoBuiltPackage.parse("vcPKG1-2.0#123");

        VersionSetRevision versionSetRevision = VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1, TestData.PKG_2, VCPKG_1, VCPKG_2))
                .build();

        BuildConfig buildConfig = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_2, VCPKG_1))
                .resolveConflicts(List.of(VCPKG_1))
                .build();

        BuildConfig buildConfig2 = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(VCPKG_2))
                .build();

        when(workspaceContext.getConfig(any())).thenReturn(EMPTY_BUILD_CONFIG);
        when(workspaceContext.getConfig(eq(TestData.PKG_1))).thenReturn(buildConfig);
        when(workspaceContext.getConfig(eq(TestData.PKG_2))).thenReturn(buildConfig2);
        when(workspaceContext.getVersionSetRevision()).thenReturn(versionSetRevision);

        ArchipelagoDependencyGraph graph = DependencyGraphGenerator.generateGraph(workspaceContext, TestData.PKG_1, DependencyTransversalType.RUNTIME);

        assertNotNull(graph);
        assertEquals(3, graph.vertexSet().size());
        assertEquals(2, graph.outgoingEdgesOf(TestData.PKG_1).size());
        assertEquals(1, graph.outgoingEdgesOf(TestData.PKG_2).size());
        assertTrue(graph.outgoingEdgesOf(TestData.PKG_2).stream().anyMatch(d -> d.getDependency().getPackage().equals(VCPKG_1)));
        assertEquals(0, graph.outgoingEdgesOf(VCPKG_1).size());
    }

    @Test
    public void testConflictResolveWithARemoveOnTheBeforeResolved() throws PackageNotLocalException, IOException, PackageNotFoundException, PackageDependencyLoopDetectedException, PackageVersionConflictException, PackageNotInVersionSetException, VersionSetNotSyncedException, LocalPackageMalformedException {
        ArchipelagoBuiltPackage VCPKG_1 = ArchipelagoBuiltPackage.parse("vcPKG1-1.0#123");
        ArchipelagoBuiltPackage VCPKG_2 = ArchipelagoBuiltPackage.parse("vcPKG1-2.0#123");

        VersionSetRevision versionSetRevision = VersionSetRevision.builder()
                .created(Instant.now())
                .packages(List.of(TestData.PKG_1, TestData.PKG_2, TestData.PKG_3, VCPKG_1, VCPKG_2))
                .build();

        BuildConfig buildConfig = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_2, VCPKG_1))
                .resolveConflicts(List.of(VCPKG_1))
                .removeDependencies(List.of(TestData.PKG_3))
                .build();

        BuildConfig buildConfig2 = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(TestData.PKG_3))
                .build();

        BuildConfig buildConfig3 = BuildConfig.builder()
                .buildSystem("Copy")
                .libraries(List.of(VCPKG_2))
                .build();

        when(workspaceContext.getConfig(any())).thenReturn(EMPTY_BUILD_CONFIG);
        when(workspaceContext.getConfig(eq(TestData.PKG_1))).thenReturn(buildConfig);
        when(workspaceContext.getConfig(eq(TestData.PKG_2))).thenReturn(buildConfig2);
        when(workspaceContext.getConfig(eq(TestData.PKG_3))).thenReturn(buildConfig3);
        when(workspaceContext.getVersionSetRevision()).thenReturn(versionSetRevision);

        ArchipelagoDependencyGraph graph = DependencyGraphGenerator.generateGraph(workspaceContext, TestData.PKG_1, DependencyTransversalType.RUNTIME);

        assertNotNull(graph);
        assertEquals(3, graph.vertexSet().size());
        assertEquals(2, graph.outgoingEdgesOf(TestData.PKG_1).size());
        assertEquals(0, graph.outgoingEdgesOf(TestData.PKG_2).size());
        assertEquals(0, graph.outgoingEdgesOf(VCPKG_1).size());
    }
}