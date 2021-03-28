package build.archipelago.maui.builder.configuration;

import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.builder.clients.UnauthorizedAuthService;
import build.archipelago.maui.builder.clients.UnauthorizedHarborClient;
import build.archipelago.maui.common.LocalGitPackageSourceProvider;
import build.archipelago.maui.common.PackageSourceProvider;
import build.archipelago.maui.common.cache.LocalPackageCacher;
import build.archipelago.maui.common.cache.PackageCacher;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.auth.AuthService;
import build.archipelago.maui.core.auth.AuthServiceImpl;
import build.archipelago.maui.core.output.ConsoleOutputWrapper;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.graph.DependencyGraphGenerator;
import build.archipelago.maui.path.MauiPath;
import build.archipelago.maui.path.recipies.*;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class ServiceConfiguration extends AbstractModule {

    @Provides
    @Singleton
    public SystemPathProvider systemPathProvider() {
        return new SystemPathProvider();
    }

    @Provides
    @Singleton
    public AuthService authService() {
        return new UnauthorizedAuthService();
    }

    @Provides
    @Singleton
    public HarborClient versionServiceClient() throws IOException {
        return new UnauthorizedHarborClient();
    }

    @Provides
    @Singleton
    public WorkspaceContextFactory workspaceContextFactory(PackageCacher packageCacher) {
        return new WorkspaceContextFactory(packageCacher);
    }

    @Provides
    @Singleton
    public PackageCacher packageCacher(HarborClient harborClient,
                                       SystemPathProvider systemPathProvider) throws IOException {
        Path workspaceDir = systemPathProvider.getWorkspaceDir();
        if (workspaceDir == null) {
            throw new RuntimeException("Was unable to find the workspace dir");
        }
        Path basePath = workspaceDir.resolve(".archipelago");

        if (!Files.exists(basePath)) {
            Files.createDirectory(basePath);
        }
        Path cachePath = basePath.resolve("cache");
        if (!Files.exists(cachePath)) {
            Files.createDirectory(cachePath);
        }
        Path tempPath = basePath.resolve("temp");
        if (!Files.exists(tempPath)) {
            Files.createDirectory(tempPath);
        }
        return new LocalPackageCacher(cachePath, tempPath, harborClient);
    }

    @Provides
    @Singleton
    public DependencyGraphGenerator dependencyGraphGenerator() {
        return new DependencyGraphGenerator();
    }

    @Provides
    @Singleton
    public MauiPath mauiPath(DependencyGraphGenerator dependencyGraphGenerator) {
        return new MauiPath(List.of(
                new BinRecipe(),
                new ClasspathRecipe(),
                new PackageRecipe(),
                new DirRecipe(),
                new JDKRecipe()
        ), dependencyGraphGenerator);
    }

    @Provides
    @Singleton
    public PackageSourceProvider packageSourceProvider() {
        return new LocalGitPackageSourceProvider();
    }

    @Provides
    @Singleton
    public OutputWrapper outputWrapper() {
        return new ConsoleOutputWrapper();
    }
}
