package build.archipelago.maui.configuration;

import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.harbor.client.*;
import build.archipelago.maui.MauiConstants;
import build.archipelago.maui.clients.UnauthorizedHarborClient;
import build.archipelago.maui.common.*;
import build.archipelago.maui.common.cache.*;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.auth.AuthService;
import build.archipelago.maui.core.auth.AuthServiceImpl;
import build.archipelago.maui.core.output.*;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.graph.DependencyGraphGenerator;
import build.archipelago.maui.core.auth.OAuthTokenResponse;
import build.archipelago.maui.path.MauiPath;
import build.archipelago.maui.path.recipies.*;
import build.archipelago.maui.utils.AuthUtil;
import com.google.common.base.Strings;
import com.google.inject.*;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Slf4j
public class ServiceConfiguration extends AbstractModule {

    private static final String ACCESS_KEY_USERNAME = "ARCHIPELAGO_ACCESS_KEY";
    private static final String ACCESS_KEY_TOKEN = "ARCHIPELAGO_ACCESS_TOKEN";

    @Provides
    @Singleton
    public SystemPathProvider systemPathProvider() {
        return new SystemPathProvider();
    }

    @Provides
    @Singleton
    public AuthService authService(
            @Named("oauth.endpoint") String oAuthEndpoint,
            @Named("oauth.clientid") String clientid) {
        return new AuthServiceImpl(clientid, oAuthEndpoint);
    }

    @Provides
    @Singleton
    public HarborClient versionServiceClient(SystemPathProvider systemPathProvider,
                                             AuthService authService,
                                             @Named("oauth.endpoint") String oAuthEndpoint,
                                             @Named("services.harbor.url") String harborEndpoint) throws IOException {
        if (!Strings.isNullOrEmpty(System.getenv(ACCESS_KEY_USERNAME))) {
            log.info("Using access key auth");
            OAuthTokenResponse oauth = authService.getToken(System.getenv(ACCESS_KEY_USERNAME), System.getenv(ACCESS_KEY_TOKEN));
            if (oauth == null) {
                log.error("The access key was not valid");
                return new UnauthorizedHarborClient();
            }
            return new RestHarborClient(harborEndpoint, oAuthEndpoint + "/oauth2/token", oauth.getAccessToken());
        }

        Path authFile = systemPathProvider.getMauiPath().resolve(".auth");
        if (!Files.exists(authFile)) {
            return new UnauthorizedHarborClient();
        }

        OAuthTokenResponse oauth = AuthUtil.getAuthSettings(systemPathProvider);
        if (oauth == null) {
            log.error("Failed to read the auth settings");
            return new UnauthorizedHarborClient();
        }

        try {
            if (authService.isTokenExpired(oauth.getAccessToken())) {
                log.info("Auth token has expired");
                if (oauth.getRefreshToken() != null && !authService.isTokenExpired(oauth.getRefreshToken())) {
                    log.info("Found valid refresh token, trying to get a new access token");
                    oauth = authService.getTokenFromRefreshToken(oauth.getRefreshToken());
                    if (oauth == null) {
                        log.warn("Failed to get new access token with the refresh token, user needs to re-auth");
                        return new UnauthorizedHarborClient();
                    }
                    log.info("Got new access token");
                    AuthUtil.saveAuthSettings(systemPathProvider, oauth);
                } else {
                    log.warn("No valid refresh token, user needs to re-auth");
                    return new UnauthorizedHarborClient();
                }
            }
            return new RestHarborClient(harborEndpoint, oAuthEndpoint + "/oauth2/token", oauth.getAccessToken());
        } catch (UnauthorizedException exp) {
            log.error("Got UnauthorizedException when trying to use refresh token", exp);
            return new UnauthorizedHarborClient();
        }
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
