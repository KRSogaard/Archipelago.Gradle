package build.archipelago.buildserver.builder.handlers;

import build.archipelago.buildserver.common.services.build.BuildService;
import build.archipelago.buildserver.common.services.build.exceptions.BuildRequestNotFoundException;
import build.archipelago.buildserver.common.services.build.models.*;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.maui.core.workspace.cache.*;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContext;
import build.archipelago.maui.core.workspace.path.DependencyTransversalType;
import build.archipelago.maui.core.workspace.path.graph.*;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import com.wewelo.sqsconsumer.exceptions.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class VersionSetBuilder {
    private VersionSetServiceClient vsClient;
    private PackageServiceClient packageServiceClient;
    private Path workspaceLocation;
    private String mauiPath;
    private BuildService buildService;
    private String buildId;

    private BuildRequest request;
    private VersionSet vs;
    private WorkspaceContext wsContext;
    private Map<ArchipelagoPackage, ArchipelagoDependencyGraph> graphs;

    public VersionSetBuilder(VersionSetServiceClient vsClient, PackageServiceClient packageServiceClient,
                             Path workspaceLocation, String mauiPath, BuildService buildService,
                             String buildId) {
        this.vsClient = vsClient;
        this.packageServiceClient = packageServiceClient;
        this.workspaceLocation = workspaceLocation;
        this.mauiPath = mauiPath;
        this.buildService = buildService;
        this.buildId = buildId;

        graphs = new HashMap<>();
    }

    public void build() throws TemporaryMessageProcessingException, PermanentMessageProcessingException {
        if (!verifyMauiIsPreset()) {
            log.error("Maui is not preset on the server, can not continue");
            throw new TemporaryMessageProcessingException();
        }

        Path workspace = createWorkspacePath(buildId);
        try {
            try {
                request = buildService.getBuildRequest(buildId);
            } catch (BuildRequestNotFoundException e) {
                throw new PermanentMessageProcessingException("The build request was not found", e);
            }
            try {
                vs = vsClient.getVersionSet(request.getVersionSet());
            } catch (VersionSetDoseNotExistsException e) {
                throw new PermanentMessageProcessingException("Version-Set dose not exists", e);
            }

            createWorkspace(request.getVersionSet(), buildId);
            syncWorkspace(workspace);
            checkOutPackagedToBuild();

            wsContext = createWorkspaceContext(workspace);
            generateGraphs();

        } finally {
            try {
                if (Files.exists(workspace) && Files.isDirectory(workspace)) {
                    try (Stream<Path> walk = Files.walk(workspace)) {
//                        walk.sorted(Comparator.reverseOrder())
//                                .map(Path::toFile)
//                                .forEach(File::delete);
                    }
                }
            } catch (IOException e) {
                log.error("Failed to delete workspace", e);
            }
        }
    }

    private void checkOutPackagedToBuild() throws TemporaryMessageProcessingException {
        for (BuildPackageDetails details : request.getBuildPackages()) {
            ProcessBuilder processBuilder = getMauiProcess();
            processBuilder.directory(workspaceLocation.toFile());
            processBuilder.command(mauiPath, "ws", "use", "-p", details.getPackageName());
            int exit;
            try {
                exit = processBuilder.start().waitFor();
                if (exit != 0) {
                    throw new TemporaryMessageProcessingException("Failed to checkout package " + details.getPackageName());
                }
            } catch (Exception e) {
                throw new TemporaryMessageProcessingException("Failed to checkout package " + details.getPackageName(), e);
            }

            Path packagePath = findPackageDir(details.getPackageName());
            if (packagePath == null) {
                throw new TemporaryMessageProcessingException("Failed to find package directory for package: " + details.getPackageName());
            }

            processBuilder = new ProcessBuilder();
            processBuilder.directory(packagePath.toFile());
            processBuilder.command("git", "checkout", details.getBranch());
            try {
                exit = processBuilder.start().waitFor();
                if (exit != 0) {
                    throw new TemporaryMessageProcessingException("Failed to checkout branch " + details.getBranch() + " package " + details.getPackageName());
                }
            } catch (Exception e) {
                throw new TemporaryMessageProcessingException("Failed to checkout branch " + details.getBranch() + " package " + details.getPackageName(), e);
            }

            processBuilder = new ProcessBuilder();
            processBuilder.directory(packagePath.toFile());
            processBuilder.command("git", "checkout", details.getCommit());
            try {
                exit = processBuilder.start().waitFor();
                if (exit != 0) {
                    throw new TemporaryMessageProcessingException("Failed to checkout commit " + details.getCommit() + " package " + details.getPackageName());
                }
            } catch (Exception e) {
                throw new TemporaryMessageProcessingException("Failed to checkout commit " + details.getCommit() + " package " + details.getPackageName(), e);
            }
        }
    }

    private Path findPackageDir(String name) throws TemporaryMessageProcessingException {
        Path packagePath = null;
        try {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(workspaceLocation)) {
                for (Path path : stream) {
                    if (Files.isDirectory(path) &&
                            path.getFileName().toString().equalsIgnoreCase(name)) {
                        packagePath = path;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            throw new TemporaryMessageProcessingException("Failed to find package directory", e);
        }
        return packagePath;
    }

    private void generateGraphs() throws TemporaryMessageProcessingException {
        for (ArchipelagoPackage target : vs.getTargets()) {
            graphs.put(target, generateGraph(wsContext, target));
        }
    }

    private List<ArchipelagoPackage> findPackageAffectedByChange()
        throws TemporaryMessageProcessingException {
        HashMap<String, ArchipelagoPackage> map = new HashMap<>();
        for (ArchipelagoPackage target : vs.getTargets()) {
            //fillMapWithAffectedPackage(changedPackage, graphs.get(target), map);
        }
        return new ArrayList<>(map.values());
    }
    private void fillMapWithAffectedPackage(ArchipelagoPackage pkg, ArchipelagoDependencyGraph graph, HashMap<String, ArchipelagoPackage> map) {
        Set<ArchipelagoPackageEdge> edges = graph.incomingEdgesOf(pkg);
        edges.stream().map(e -> e.getDependency().getPackage()).forEach(p -> {
            String key = p.getNameVersion().toLowerCase();
            if (!map.containsKey(key)) {
                map.put(key, p);
                fillMapWithAffectedPackage(p, graph, map);
            }
        });
    }

    private ArchipelagoDependencyGraph generateGraph(WorkspaceContext wsContext, ArchipelagoPackage pkg) throws TemporaryMessageProcessingException {
        try {
            return DependencyGraphGenerator.generateGraph(wsContext, pkg, DependencyTransversalType.BUILD_TOOLS);
        } catch (Exception e) {
            log.error(String.format("Error while generating graph for %s", pkg.getNameVersion()), e);
            throw new TemporaryMessageProcessingException(String.format("Error while generating graph for %s", pkg.getNameVersion()), e);
        }
    }

    private WorkspaceContext createWorkspaceContext(Path wsPath) throws TemporaryMessageProcessingException {
        Path cachePath = wsPath.resolve(".archipelago").resolve("cache");
        Path tempPath = wsPath.resolve(".archipelago").resolve("temp");
        PackageCacher packageCacher = null;
        try {
            packageCacher = new LocalPackageCacher(cachePath, tempPath, packageServiceClient);
        } catch (IOException e) {
            throw new TemporaryMessageProcessingException("Where unable to create cache or temp dirs", e);
        }
        return new WorkspaceContext(wsPath, vsClient, packageCacher);
    }

    private Path createWorkspacePath(String buildId) {
        return workspaceLocation.resolve(buildId);
    }

    private void syncWorkspace(Path workspace) throws TemporaryMessageProcessingException {
        ProcessBuilder processBuilder = getMauiProcess();
        processBuilder.directory(workspace.toFile());
        processBuilder.command(mauiPath, "ws", "sync");
        try {
            int wsCreateExit = processBuilder.start().waitFor();
            if (wsCreateExit != 0) {
                log.error("Got non zero return code when creating workspace: {}", wsCreateExit);
                throw new TemporaryMessageProcessingException();
            }
        } catch (Exception e) {
            log.error("Exception while creating workspace");
            throw new TemporaryMessageProcessingException();
        }
    }

    private void createWorkspace(String versionSet, String buildId) throws TemporaryMessageProcessingException {
        ProcessBuilder processBuilder = getMauiProcess();
        processBuilder.directory(workspaceLocation.toFile());
        processBuilder.command(mauiPath, "ws", "create", "-vs", versionSet, "--name", buildId);
        try {
            int wsCreateExit = processBuilder.start().waitFor();
            if (wsCreateExit != 0) {
                log.error("Got non zero return code when creating workspace: {}", wsCreateExit);
                throw new TemporaryMessageProcessingException();
            }
        } catch (Exception e) {
            log.error("Exception while creating workspace");
            throw new TemporaryMessageProcessingException();
        }
    }

    private boolean verifyMauiIsPreset() {
        try {
            ProcessBuilder processBuilder = getMauiProcess();
            processBuilder.directory(workspaceLocation.toFile());
            processBuilder.command(mauiPath, "version");
            int versionExit = processBuilder.start().waitFor();
            return versionExit == 0;
        } catch (Exception e) {
            log.error("Failed maui preset check with exception", e);
            return false;
        }
    }

    private ProcessBuilder getMauiProcess() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.environment().put("MAUI_USE_WORKSPACE_CACHE", "true");
        return processBuilder;
    }
}
