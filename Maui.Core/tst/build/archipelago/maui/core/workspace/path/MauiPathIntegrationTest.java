package build.archipelago.maui.core.workspace.path;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.maui.core.TestData;
import build.archipelago.maui.core.exceptions.*;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContext;
import build.archipelago.maui.core.workspace.models.BuildConfig;
import build.archipelago.maui.core.workspace.path.graph.DependencyGraphGenerator;
import build.archipelago.maui.core.workspace.path.recipies.Recipe;
import com.google.common.collect.ImmutableList;
import org.junit.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class MauiPathIntegrationTest {

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
    public void testPathGenerations() throws Exception {
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
        when(workspaceContext.isPackageInVersionSet(any())).thenReturn(true);
        when(workspaceContext.getPackageRoot(any())).thenReturn(Paths.get("c:/test"));

        MauiPath mauiPath = new MauiPath(List.of(new TestRecipe()));
        ImmutableList<String> list = mauiPath.getPaths(workspaceContext, TestData.PKG_1, "all.test");
        assertEquals(4, list.size());
    }

    @Test
    public void testPathGenerationsFromTargetPackage() throws Exception {
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
        when(workspaceContext.isPackageInVersionSet(any())).thenReturn(true);
        when(workspaceContext.getPackageRoot(any())).thenReturn(Paths.get("c:/test"));

        MauiPath mauiPath = new MauiPath(List.of(new TestRecipe()));
        ImmutableList<String> list = mauiPath.getPaths(workspaceContext,
                TestData.PKG_1,
                "["+TestData.PKG_2.getNameVersion()+ "]all.test");
        assertEquals(2, list.size());
    }

    private static class TestRecipe implements Recipe {

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public List<String> execute(ArchipelagoPackage pkg, WorkspaceContext workspaceContext) {
            try {
                return List.of(workspaceContext.getPackageRoot(pkg).toRealPath().toString());
            } catch (IOException e) {
                return List.of(e.getMessage());
            } catch (PackageNotLocalException e) {
                return List.of(e.getMessage());
            }
        }
    }
}
