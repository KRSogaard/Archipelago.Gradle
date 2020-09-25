package build.archipelago.maui.configuration;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.concurrent.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.versionset.*;
import build.archipelago.maui.core.workspace.WorkspaceSyncer;
import build.archipelago.maui.core.workspace.cache.*;
import build.archipelago.maui.utils.SystemUtil;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.rest.RestPackageServiceClient;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import build.archipelago.versionsetservice.client.model.CreateVersionSetRequest;
import build.archipelago.versionsetservice.client.rest.RestVersionSetServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Configuration
public class ServiceConfiguration {

    @Bean
    public VersionServiceClient versionServiceClient(@Value("${services.versionset.url}") String vsEndpoint) {
        return new RestVersionSetServiceClient(vsEndpoint);
    }

    @Bean
    public PackageServiceClient packageServiceClient(@Value("${services.packages.url}") String pkgEndpoint) {
        return new RestPackageServiceClient(pkgEndpoint);
    }

    @Bean
    public PackageCacher packageCacher(PackageServiceClient packageServiceClient) throws IOException {
        Path cachePath = SystemUtil.getCachePath();
        if (!Files.exists(cachePath)) {
            Files.createDirectory(cachePath);
        }
        Path tempPath = SystemUtil.getMauiPath().resolve("temp");
        if (!Files.exists(tempPath)) {
            Files.createDirectory(tempPath);
        }
        return new LocalPackageCacher(cachePath, tempPath, packageServiceClient);
    }

    @Bean
    public WorkspaceSyncer workspaceSyncer(PackageCacher packageCacher,
                                           VersionServiceClient versionServiceClient) {
        BlockingExecutorServiceFactory executorServiceFactory = new BlockingExecutorServiceFactory();
        executorServiceFactory.setMaximumPoolSize(4);
        return new WorkspaceSyncer(packageCacher, versionServiceClient, executorServiceFactory);
    }
}
