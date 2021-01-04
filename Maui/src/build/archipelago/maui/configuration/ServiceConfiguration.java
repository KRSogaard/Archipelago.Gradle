package build.archipelago.maui.configuration;

import build.archipelago.harbor.client.HarborClient;
import build.archipelago.harbor.client.RestHarborClient;
import build.archipelago.maui.MauiConstants;
import build.archipelago.maui.clients.UnauthorizedHarborClient;
import build.archipelago.maui.common.LocalGitPackageSourceProvider;
import build.archipelago.maui.common.PackageSourceProvider;
import build.archipelago.maui.common.cache.LocalPackageCacher;
import build.archipelago.maui.common.cache.PackageCacher;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.output.ConsoleOutputWrapper;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.graph.DependencyGraphGenerator;
import build.archipelago.maui.models.OAuthTokenResponse;
import build.archipelago.maui.path.MauiPath;
import build.archipelago.maui.path.recipies.BinRecipe;
import build.archipelago.maui.path.recipies.PackageRecipe;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
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
    public HarborClient versionServiceClient(SystemPathProvider systemPathProvider,
                                             @Named("oauth.endpoint") String oAuthEndpoint,
                                             @Named("oauth.audience") String audience,
                                             @Named("services.harbor.url") String harborEndpoint) throws IOException {
        Path authFile = systemPathProvider.getMauiPath().resolve(".auth");
        if (!Files.exists(authFile)) {
            return new UnauthorizedHarborClient();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        OAuthTokenResponse oauth;
        try {
            String authFileContent = Files.readString(authFile);
            oauth = objectMapper.readValue(authFileContent, OAuthTokenResponse.class);
        } catch (Exception exp) {
            log.error("The auth file is corrupt: " + Files.readString(authFile));
            return new UnauthorizedHarborClient();
        }
        if (oauth == null) {
            log.error("The auth file is corrupt: " + Files.readString(authFile));
            return new UnauthorizedHarborClient();
        }
        return new RestHarborClient(harborEndpoint, oAuthEndpoint + "/oauth/token", oauth.getAccessToken(), audience);
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
        Path basePath = systemPathProvider.getMauiPath();
        if ("true".equalsIgnoreCase(System.getenv(MauiConstants.ENV_USE_LOCAL_WORKSPACE_CACHE))) {
            Path workspaceDir = systemPathProvider.getWorkspaceDir();
            if (workspaceDir == null) {
                throw new RuntimeException("Was unable to find the workspace dir");
            }
            basePath = workspaceDir.resolve(".archipelago");
        }
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
                new PackageRecipe()
        ), dependencyGraphGenerator);
    }

    @Provides
    @Singleton
    public PackageSourceProvider packageSourceProvider(@Named("sourceprovider") String serviceProvider,
                                                       @Named("sourceprovider.git.base") String gitBase,
                                                       @Named("sourceprovider.git.group") String gitGroup) {
        return new LocalGitPackageSourceProvider(gitBase, gitGroup);
    }

    @Provides
    @Singleton
    public OutputWrapper outputWrapper() {
        return new ConsoleOutputWrapper();
    }
}
