package build.archipelago.buildserver.builder.handlers;

import build.archipelago.buildserver.common.services.build.BuildService;
import build.archipelago.buildserver.common.services.build.exceptions.BuildRequestNotFoundException;
import build.archipelago.buildserver.common.services.build.models.*;
import build.archipelago.common.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.maui.core.exceptions.*;
import build.archipelago.maui.core.workspace.WorkspaceConstants;
import build.archipelago.maui.core.workspace.cache.*;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContext;
import build.archipelago.maui.core.workspace.models.BuildConfig;
import build.archipelago.maui.core.workspace.path.DependencyTransversalType;
import build.archipelago.maui.core.workspace.path.graph.*;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.*;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import com.wewelo.sqsconsumer.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.jgrapht.alg.util.Pair;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.*;

@Slf4j
public class VersionSetBuilder {
    private VersionSetServiceClient vsClient;
    private PackageServiceClient packageServiceClient;
    private Path buildLocation;
    private String mauiPath;
    private BuildService buildService;
    private String buildId;

    private Path workspaceLocation;
    private BuildRequest request;
    private VersionSet vs;
    private WorkspaceContext wsContext;
    private Map<ArchipelagoPackage, ArchipelagoDependencyGraph> graphs;

    private List<ArchipelagoPackage> getBuildPackages;
    private Map<ArchipelagoPackage, Boolean> doneBuilding;
    private Map<ArchipelagoPackage, List<ArchipelagoPackage>> buildDependicies;
    private Queue<ArchipelagoPackage> buildQueue;

    public VersionSetBuilder(VersionSetServiceClient vsClient, PackageServiceClient packageServiceClient,
                             Path buildLocation, String mauiPath,
                             BuildService buildService, String buildId) {
        this.vsClient = vsClient;
        this.packageServiceClient = packageServiceClient;
        this.buildLocation = buildLocation;
        this.mauiPath = mauiPath;
        this.buildService = buildService;
        this.buildId = buildId;

        graphs = new HashMap<>();
        buildQueue = new ConcurrentLinkedDeque<>();
        buildDependicies = new HashMap<>();
        doneBuilding = new HashMap<>();
    }

    public void build() throws TemporaryMessageProcessingException, PermanentMessageProcessingException {
        if (!verifyMauiIsPreset()) {
            log.error("Maui is not preset on the server, can not continue");
            throw new TemporaryMessageProcessingException();
        }

        workspaceLocation = createWorkspacePath(buildId);
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
            syncWorkspace();
            // We need to check out the packages before we create the graphs, the new build may have changed the graph
            // they may even have change the version number
            checkOutPackagedToBuild();
            createWorkspaceContext();
            generateGraphs();
            getBuildPackages = wsContext.getLocalArchipelagoPackages();

            checkoutAffectedPackages(getBuildPackages);
            // Recreate the workspace context with the newly checkout packages
            createWorkspaceContext();
            generateGraphs();

            Map<ArchipelagoPackage, Boolean> lookupMap = new HashMap<>();
            wsContext.getLocalArchipelagoPackages().forEach(pkg -> lookupMap.put(pkg, true));
            for (ArchipelagoPackage pkg : wsContext.getLocalArchipelagoPackages()) {
                BuildConfig config = wsContext.getConfig(pkg);
                List<ArchipelagoPackage> dependencies = config.getAllDependencies().stream()
                        .filter(lookupMap::containsKey).collect(Collectors.toList());
                buildDependicies.put(pkg, dependencies);
                buildQueue.add(pkg);
            }

            ArchipelagoPackage pkg = getNextPackageToBuild();
            while(pkg != null) {
                log.info("Building " + pkg);
                buildPackage(pkg);
                pkg = getNextPackageToBuild();
            }

            Map<ArchipelagoPackage, Pair<String, String>> gitMap = new HashMap<>();
            for (BuildPackageDetails bpd : request.getBuildPackages()) {
                Optional<ArchipelagoPackage> archipelagoPackage = getBuildPackages.stream()
                        .filter(p -> p.getName().equalsIgnoreCase(bpd.getPackageName()))
                        .findFirst();
                if (archipelagoPackage.isEmpty()) {
                    log.error("Could not find the archipelago package \"{}\" from build details", bpd.getPackageName());
                    throw new TemporaryMessageProcessingException();
                }
                gitMap.put(archipelagoPackage.get(), new Pair<>(bpd.getBranch(), bpd.getCommit()));
            }
            // Build are done
            List<ArchipelagoBuiltPackage> newBuildPackage = new ArrayList<>();
            for (ArchipelagoPackage builtPackage : getBuildPackages) {
                Path zip = prepareBuildZip(builtPackage);
                String configContent = Files.readString(wsContext.getPackageRoot(builtPackage).resolve(WorkspaceConstants.BUILD_FILE_NAME));
                if (!gitMap.containsKey(builtPackage)) {
                    log.error("Build package {} was not in the git map", builtPackage);
                    throw new TemporaryMessageProcessingException();
                }
                Pair<String, String> gitInfo = gitMap.get(builtPackage);
                String buildHash =  packageServiceClient.uploadBuiltArtifact(UploadPackageRequest.builder()
                        .config(configContent)
                        .pkg(builtPackage)
                        .gitBranch(gitInfo.getFirst())
                        .gitCommit(gitInfo.getSecond())
                        .build(), zip);
                newBuildPackage.add(new ArchipelagoBuiltPackage(builtPackage, buildHash));
            }

            List<ArchipelagoBuiltPackage> newRevision = wsContext.getVersionSetRevision().getPackages()
                    .stream().filter(rp -> newBuildPackage.stream().noneMatch(p -> rp.getNameVersion().equalsIgnoreCase(p.getNameVersion())))
                    .collect(Collectors.toList());
            newRevision.addAll(newBuildPackage);

            vsClient.createVersionRevision(wsContext.getVersionSet(), newRevision);

        } catch (PackageNotFoundException e) {
            log.error("Unhandled error", e);
            throw new TemporaryMessageProcessingException(e);
        } catch (VersionSetNotSyncedException e) {
            log.error("Unhandled error", e);
            throw new TemporaryMessageProcessingException(e);
        } catch (PackageNotLocalException e) {
            log.error("Unhandled error", e);
            throw new TemporaryMessageProcessingException(e);
        } catch (PackageNotInVersionSetException e) {
            log.error("Unhandled error", e);
            throw new TemporaryMessageProcessingException(e);
        } catch (IOException e) {
            log.error("Unhandled error", e);
            throw new TemporaryMessageProcessingException(e);
        } catch (LocalPackageMalformedException e) {
            log.error("Unhandled error", e);
            throw new TemporaryMessageProcessingException(e);
        } catch (MissingTargetPackageException e) {
            log.error("Unhandled error", e);
            throw new TemporaryMessageProcessingException(e);
        } catch (VersionSetDoseNotExistsException e) {
            log.error("Unhandled error", e);
            throw new TemporaryMessageProcessingException(e);
        } finally {
            try {
                if (Files.exists(workspaceLocation) && Files.isDirectory(workspaceLocation)) {
                    try (Stream<Path> walk = Files.walk(workspaceLocation)) {
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

    private Path prepareBuildZip(ArchipelagoPackage builtPackage) throws PackageNotLocalException, TemporaryMessageProcessingException, IOException {
        Path buildPath = wsContext.getPackageBuildPath(builtPackage);
        Path zipFolder = workspaceLocation.resolve("buildZips");
        if (!Files.exists(zipFolder)) {
            Files.createDirectory(zipFolder);
        }
        Path zipPath = workspaceLocation.resolve("buildZips").resolve(UUID.randomUUID().toString() + ".zip");
        try {
            ZipFile zip =new ZipFile(zipPath.toFile());
            try (Stream<Path> walk = Files.walk(buildPath, 1, FileVisitOption.FOLLOW_LINKS)) {
                List<File> files = walk.sorted(Comparator.reverseOrder())
                        .filter(p -> !p.equals(buildPath))
                        .map(Path::toFile)
                        .collect(Collectors.toList());
                for (File f : files) {
                    if (f.isDirectory()) {
                        zip.addFolder(f);
                    } else {
                        zip.addFile(f);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Unhandled error", e);
            throw new TemporaryMessageProcessingException(e);
        }
        return zipPath;
    }

    private void buildPackage(ArchipelagoPackage pkg) throws TemporaryMessageProcessingException {
        try {
            ProcessBuilder processBuilder = getMauiProcess();
            processBuilder.directory(wsContext.getPackageRoot(pkg).toFile());
            processBuilder.command(mauiPath, "build");
            int wsCreateExit = processBuilder.start().waitFor();
            if (wsCreateExit != 0) {
                log.error("Build failed: {}", wsCreateExit);
                throw new TemporaryMessageProcessingException();
            }
            doneBuilding.put(pkg, true);
        } catch (PackageNotLocalException e) {
            log.error("Unhandled error", e);
            throw new TemporaryMessageProcessingException(e);
        } catch (InterruptedException e) {
            log.error("Unhandled error", e);
            throw new TemporaryMessageProcessingException(e);
        } catch (IOException e) {
            log.error("Unhandled error", e);
            throw new TemporaryMessageProcessingException(e);
        }
    }

    private ArchipelagoPackage getNextPackageToBuild() {
        while(buildQueue.size() > 0) {
            ArchipelagoPackage pkg = buildQueue.poll();
            if (pkg == null) {
                return null;
            }
            if (isAllDependenciesBuilt(pkg)) {
                return pkg;
            }
        }
        return null;
    }
    private boolean isAllDependenciesBuilt(ArchipelagoPackage pkg) {
        List<ArchipelagoPackage> dependencies = buildDependicies.get(pkg);
        for (ArchipelagoPackage d : dependencies) {
            if (!doneBuilding.get(d)) {
                return false;
            }
        }
        return true;
    }

    private void checkoutAffectedPackages(List<ArchipelagoPackage> getBuildPackages) throws TemporaryMessageProcessingException {
        List<ArchipelagoPackage> affectedPackages = findPackageAffectedByChange();
        for(ArchipelagoPackage pkg : affectedPackages.stream()
                .filter(p -> getBuildPackages.stream().noneMatch(bp -> bp.equals(p)))
                .collect(Collectors.toList())) {
            ArchipelagoBuiltPackage builtPackage = getPackageFromRevision(pkg);
            if (builtPackage == null) {
                throw new TemporaryMessageProcessingException("Affected package " + pkg + " was not in the version set revision");
            }
            GetPackageBuildResponse response;
            try {
                response = packageServiceClient.getPackageBuild(builtPackage);
            } catch (PackageNotFoundException e) {
                throw new TemporaryMessageProcessingException("The package " + builtPackage + " was not found on the package server", e);
            }

            checkOutPackage(builtPackage.getName(), response.getGitBranch(), response.getGitCommit());
        }
    }

    private ArchipelagoBuiltPackage getPackageFromRevision(ArchipelagoPackage pkg) throws TemporaryMessageProcessingException {
        try {
            Optional<ArchipelagoBuiltPackage> optional = wsContext.getVersionSetRevision().getPackages().stream()
                    .filter(b -> b.equals(pkg)).findFirst();
            if (optional.isPresent()) {
                return optional.get();
            }
        } catch (IOException e) {
            log.error("An unknown IO Exception occured", e);
            throw new TemporaryMessageProcessingException(e);
        } catch (VersionSetNotSyncedException e) {
            log.error("The version-set was not synced", e);
            throw new TemporaryMessageProcessingException(e);
        }
        return null;
    }

    private void checkOutPackagedToBuild() throws TemporaryMessageProcessingException {
        for (BuildPackageDetails details : request.getBuildPackages()) {
            checkOutPackage(details.getPackageName(), details.getBranch(), details.getCommit());
        }
    }

    private void checkOutPackage(String packageName, String branch, String commit) throws TemporaryMessageProcessingException {
        ProcessBuilder processBuilder = getMauiProcess();
        processBuilder.directory(workspaceLocation.toFile());
        processBuilder.command(mauiPath, "ws", "use", "-p", packageName);
        int exit;
        try {
            exit = processBuilder.start().waitFor();
            if (exit != 0) {
                throw new TemporaryMessageProcessingException("Failed to checkout package " + packageName);
            }
        } catch (Exception e) {
            throw new TemporaryMessageProcessingException("Failed to checkout package " + packageName, e);
        }

        Path packagePath = findPackageDir(packageName);
        if (packagePath == null) {
            throw new TemporaryMessageProcessingException("Failed to find package directory for package: " + packageName);
        }

        processBuilder = new ProcessBuilder();
        processBuilder.directory(packagePath.toFile());
        processBuilder.command("git", "checkout", branch);
        try {
            exit = processBuilder.start().waitFor();
            if (exit != 0) {
                throw new TemporaryMessageProcessingException("Failed to checkout branch " + branch + " package " + packageName);
            }
        } catch (Exception e) {
            throw new TemporaryMessageProcessingException("Failed to checkout branch " + branch + " package " + packageName, e);
        }

        processBuilder = new ProcessBuilder();
        processBuilder.directory(packagePath.toFile());
        processBuilder.command("git", "checkout", commit);
        try {
            exit = processBuilder.start().waitFor();
            if (exit != 0) {
                throw new TemporaryMessageProcessingException("Failed to checkout commit " + commit + " package " + packageName);
            }
        } catch (Exception e) {
            throw new TemporaryMessageProcessingException("Failed to checkout commit " + commit + " package " + packageName, e);
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
        DependencyGraphGenerator.clearCache();
        for (ArchipelagoPackage target : vs.getTargets()) {
            graphs.put(target, generateGraph(wsContext, target));
        }
    }

    private List<ArchipelagoPackage> findPackageAffectedByChange()
        throws TemporaryMessageProcessingException {
        HashMap<String, ArchipelagoPackage> map = new HashMap<>();
        for (ArchipelagoPackage target : vs.getTargets()) {
            // We can use getLocalArchipelagoPackages as we at this point only have checked out the packages with changes
            for (ArchipelagoPackage changedPackage : wsContext.getLocalArchipelagoPackages()) {
                if (isPackageInGraph(changedPackage, graphs.get(target))) {
                    fillMapWithAffectedPackage(changedPackage, graphs.get(target), map);
                }
            }
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
    private boolean isPackageInGraph(ArchipelagoPackage pkg, ArchipelagoDependencyGraph graph) {
        return graph.vertexSet().stream().anyMatch(v -> v.equals(pkg));
    }

    private ArchipelagoDependencyGraph generateGraph(WorkspaceContext wsContext, ArchipelagoPackage pkg) throws TemporaryMessageProcessingException {
        try {
            return DependencyGraphGenerator.generateGraph(wsContext, pkg, DependencyTransversalType.ALL);
        } catch (Exception e) {
            log.error(String.format("Error while generating graph for %s", pkg.getNameVersion()), e);
            throw new TemporaryMessageProcessingException(String.format("Error while generating graph for %s", pkg.getNameVersion()), e);
        }
    }

    private void createWorkspaceContext() throws TemporaryMessageProcessingException {
        Path cachePath = workspaceLocation.resolve(".archipelago").resolve("cache");
        Path tempPath = workspaceLocation.resolve(".archipelago").resolve("temp");
        PackageCacher packageCacher = null;
        try {
            packageCacher = new LocalPackageCacher(cachePath, tempPath, packageServiceClient);
        } catch (IOException e) {
            throw new TemporaryMessageProcessingException("Where unable to create cache or temp dirs", e);
        }
        WorkspaceContext ws = new WorkspaceContext(workspaceLocation, vsClient, packageCacher);
        try {
            ws.load();
        } catch (IOException e) {
            log.error("Failed to load the workspace context");
            throw new TemporaryMessageProcessingException(e);
        }
        wsContext = ws;
    }

    private Path createWorkspacePath(String buildId) {
        return buildLocation.resolve(buildId);
    }

    private void syncWorkspace() throws TemporaryMessageProcessingException {
        ProcessBuilder processBuilder = getMauiProcess();
        processBuilder.directory(workspaceLocation.toFile());
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
        ProcessBuilder processBuilder = getMauiProcess(false);
        processBuilder.directory(buildLocation.toFile());
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
            ProcessBuilder processBuilder = getMauiProcess(false);
            processBuilder.directory(buildLocation.toFile());
            processBuilder.command(mauiPath, "version");
            int versionExit = processBuilder.start().waitFor();
            return versionExit == 0;
        } catch (Exception e) {
            log.error("Failed maui preset check with exception", e);
            return false;
        }
    }

    private ProcessBuilder getMauiProcess() {
        return getMauiProcess(true);
    }

    private ProcessBuilder getMauiProcess(boolean useWorkspaceCache) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        if (useWorkspaceCache) {
            processBuilder.environment().put("MAUI_USE_WORKSPACE_CACHE", "true");
        }
        return processBuilder;
    }
}
