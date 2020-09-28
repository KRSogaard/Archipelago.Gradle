package build.archipelago.maui.core.workspace.path;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.maui.core.exceptions.VersionSetNotSyncedException;
import build.archipelago.maui.core.workspace.ConfigProvider;
import build.archipelago.maui.core.workspace.cache.PackageCacher;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContext;
import build.archipelago.maui.core.workspace.path.graph.DependencyGraphGenerator;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.regex.Pattern;

@Slf4j
public class MauiPath {

    private static final Pattern re = Pattern.compile("^(\\[([^\\]]+)\\])?([^.]+)\\.(.+)");

    private PackageCacher packageCacher;
    private VersionServiceClient versionServiceClient;

    public MauiPath(PackageCacher packageCacher, VersionServiceClient versionServiceClient) {
        this.packageCacher = packageCacher;
        this.versionServiceClient = versionServiceClient;
    }

    // It returns string instead of Path to allow recipes to be creative wit the usage
    public ImmutableSet<String> getPaths(WorkspaceContext workspaceContext, ArchipelagoPackage targetPackage, String pathLine)
            throws IOException, VersionSetNotSyncedException {

        ConfigProvider configProvider = new ConfigProvider(workspaceContext, packageCacher);
        DependencyGraphGenerator graphGenerator = new DependencyGraphGenerator(configProvider, workspaceContext.getVersionSetRevision());

        var pathBuilder = ImmutableSet.<String>builder();
        for (String request : pathLine.split(";")) {
            log.info("Generating graph for: {}", request);
            // TODO parese incoming pathline
        }

        return pathBuilder.build();
    }
}
