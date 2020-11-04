package build.archipelago.buildserver.builder.handlers;

import build.archipelago.account.common.AccountService;
import build.archipelago.account.common.exceptions.AccountNotFoundException;
import build.archipelago.account.common.models.AccountDetails;
import build.archipelago.buildserver.builder.*;
import build.archipelago.buildserver.builder.clients.InternalHarborClientFactory;
import build.archipelago.buildserver.common.services.build.*;
import build.archipelago.buildserver.common.services.build.exceptions.BuildRequestNotFoundException;
import build.archipelago.buildserver.common.services.build.models.*;
import build.archipelago.common.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.common.WorkspaceConstants;
import build.archipelago.maui.common.cache.*;
import build.archipelago.maui.common.contexts.WorkspaceContext;
import build.archipelago.maui.common.models.BuildConfig;
import build.archipelago.maui.graph.*;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.*;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import com.wewelo.sqsconsumer.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.apache.logging.log4j.util.Strings;
import org.jgrapht.alg.util.Pair;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.*;

@Slf4j
public class VersionSetBuilder {
    private InternalHarborClientFactory internalHarborClientFactory;
    private PackageServiceClient packageServiceClient;
    private VersionSetServiceClient versionSetServiceClient;
    private HarborClient harborClient;
    private Path buildLocation;
    private BuildService buildService;
    private String buildId;
    private MauiWrapper maui;
    private AccountService accountService;
    private DependencyGraphGenerator dependencyGraphGenerator;

    private Path workspaceLocation;
    private ArchipelagoBuild request;
    private AccountDetails accountDetails;
    private VersionSet vs;
    private WorkspaceContext wsContext;
    private Map<ArchipelagoPackage, ArchipelagoDependencyGraph> graphs;

    private List<ArchipelagoPackage> getBuildPackages;
    private Map<ArchipelagoPackage, Boolean> doneBuilding;
    private Map<ArchipelagoPackage, List<ArchipelagoPackage>> buildDependicies;
    private Queue<ArchipelagoPackage> buildQueue;
    private boolean failedBuild = false;

    public VersionSetBuilder(InternalHarborClientFactory internalHarborClientFactory,
                             VersionSetServiceClient versionSetServiceClient,
                             PackageServiceClient packageServiceClient,
                             Path buildLocation, BuildService buildService, MauiWrapper maui,
                             AccountService accountService, String buildId) {
        this.internalHarborClientFactory = internalHarborClientFactory;
        this.versionSetServiceClient = versionSetServiceClient;
        this.packageServiceClient = packageServiceClient;
        this.buildLocation = buildLocation;
        this.buildService = buildService;
        this.buildId = buildId;
        this.maui = maui;
        this.accountService = accountService;

        graphs = new HashMap<>();
        buildQueue = new ConcurrentLinkedDeque<>();
        buildDependicies = new HashMap<>();
        doneBuilding = new HashMap<>();
        dependencyGraphGenerator = new DependencyGraphGenerator();
    }

    public void build() throws TemporaryMessageProcessingException, PermanentMessageProcessingException {
        if (!maui.verifyMauiIsPreset()) {
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
                accountDetails = accountService.getAccountDetails(request.getAccountId());
            } catch (AccountNotFoundException e) {
                throw new PermanentMessageProcessingException("The build account was not found", e);
            }

            harborClient = internalHarborClientFactory.create(request.getAccountId());
            try {
                vs = harborClient.getVersionSet(request.getVersionSet());
            } catch (VersionSetDoseNotExistsException e) {
                throw new PermanentMessageProcessingException("Version-set dose not exists", e);
            }

            stage_prepare();
            stage_packages();
            stage_publish();
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

    private void stage_prepare() throws PermanentMessageProcessingException {
        StageLog stageLog = new StageLog();
        try{
            stageLog.addInfo("Build preparations started");
            buildService.setBuildStatus(buildId, BuildStage.PREPARE, BuildStatus.IN_PROGRESS);
            createWorkspace(request.getVersionSet(), buildId);
            syncWorkspace();
            // We need to check out the packages before we create the graphs, the new build may have changed the graph
            // they may even have change the version number
            checkOutPackagedToBuild();
            wsContext = createWorkspaceContext();
            generateGraphs();
            getBuildPackages = wsContext.getLocalArchipelagoPackages();

            for (ArchipelagoPackage pkg : wsContext.getLocalArchipelagoPackages()) {
                buildService.setPackageStatus(buildId, pkg.getNameVersion(), BuildStatus.WAITING);
            }

            checkoutAffectedPackages(getBuildPackages);
            // Recreate the workspace context with the newly checkout packages
            wsContext = createWorkspaceContext();
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

            buildService.setBuildStatus(buildId, BuildStage.PREPARE, BuildStatus.FINISHED);
            stageLog.addInfo("Build preparations finished");
        } catch (Exception e) {
            log.error("The prepare stage failed with an exception", e);
            buildService.setBuildStatus(buildId, BuildStage.PREPARE, BuildStatus.FAILED);
            throw new PermanentMessageProcessingException(e);
        } finally {
            if (stageLog.hasLogs()) {
                buildService.uploadStageLog(buildId, BuildStage.PREPARE, stageLog.getLogs());
            }
        }
    }

    private void stage_packages() {
        StageLog stageLog = new StageLog();
        try {
            buildService.setBuildStatus(buildId, BuildStage.PACKAGES, BuildStatus.IN_PROGRESS);
            ArchipelagoPackage pkg = getNextPackageToBuild();
            while (pkg != null) {
                log.info("Building " + pkg);
                buildPackage(pkg);
                pkg = getNextPackageToBuild();
            }
            if (!failedBuild) {
                buildService.setBuildStatus(buildId, BuildStage.PACKAGES, BuildStatus.FINISHED);
            } else {
                buildService.setBuildStatus(buildId, BuildStage.PACKAGES, BuildStatus.FAILED);
            }
        } finally {
            if (stageLog.hasLogs()) {
                buildService.uploadStageLog(buildId, BuildStage.PACKAGES, stageLog.getLogs());
            }
        }
    }

    private void stage_publish() throws PermanentMessageProcessingException {
        if (request.isDryRun()) {
            return;
        }
        StageLog stageLog = new StageLog();
        try {
            buildService.setBuildStatus(buildId, BuildStage.PUBLISHING, BuildStatus.IN_PROGRESS);
            Map<ArchipelagoPackage, Pair<String, String>> gitMap = new HashMap<>();
            for (BuildPackageDetails bpd : request.getBuildPackages()) {
                Optional<ArchipelagoPackage> archipelagoPackage = getBuildPackages.stream()
                        .filter(p -> p.getName().equalsIgnoreCase(bpd.getPackageName()))
                        .findFirst();
                if (archipelagoPackage.isEmpty()) {
                    throw new IOException(String.format("Could not find the archipelago package \"%s\" in build details", bpd.getPackageName()));
                }
                gitMap.put(archipelagoPackage.get(), new Pair<>(bpd.getBranch(), bpd.getCommit()));
            }

            // Build are done
            List<ArchipelagoBuiltPackage> newBuildPackage = new ArrayList<>();
            for (ArchipelagoPackage builtPackage : getBuildPackages) {
                if (!gitMap.containsKey(builtPackage)) {
                    log.error("Build package {} was not in the git map", builtPackage);
                    throw new IOException(String.format("Build package %s was not in the git map", builtPackage));
                }
                Pair<String, String> gitInfo = gitMap.get(builtPackage);
                String buildHash = null;

                ArchipelagoBuiltPackage previousBuilt = getPreviousBuild(builtPackage.getName(), gitInfo.getFirst(), gitInfo.getSecond());
                if (previousBuilt != null) {
                    buildHash = previousBuilt.getHash();
                }

                if (buildHash == null) {
                    try {
                        String configContent = null;
                        try {
                            configContent = Files.readString(wsContext.getPackageRoot(builtPackage).resolve(WorkspaceConstants.BUILD_FILE_NAME));
                        } catch (PackageNotLocalException e) {
                            throw new ArchipelagoException("Where not able to find the package dir for " + builtPackage, e);
                        }

                        Path zip = prepareBuildZip(builtPackage);
                        buildHash = packageServiceClient.uploadBuiltArtifact(accountDetails.getId(), UploadPackageRequest.builder()
                                .config(configContent)
                                .pkg(builtPackage)
                                .gitBranch(gitInfo.getFirst())
                                .gitCommit(gitInfo.getSecond())
                                .build(), zip);
                    } catch (PackageNotFoundException e) {
                        throw new ArchipelagoException(String.format("The package %s no longer exists, was it deleted while building?", builtPackage), e);
                    }
                }

                newBuildPackage.add(new ArchipelagoBuiltPackage(builtPackage, buildHash));
            }

            List<ArchipelagoBuiltPackage> newRevision = null;
            try {
                newRevision = wsContext.getVersionSetRevision().getPackages()
                        .stream().filter(rp -> newBuildPackage.stream().noneMatch(p -> rp.getNameVersion().equalsIgnoreCase(p.getNameVersion())))
                        .collect(Collectors.toList());
            } catch (VersionSetNotSyncedException e) {
                throw new ArchipelagoException("The workspace had not been synced", e);
            }
            newRevision.addAll(newBuildPackage);

            if (!doseRevisionHaveChanges(wsContext.getVersionSetRevision().getPackages(), newRevision)) {
                log.warn("There are no changes to the version set");
                buildService.setBuildStatus(buildId, BuildStage.PUBLISHING, BuildStatus.FAILED);
                throw new PermanentMessageProcessingException("There are no changes to the version set");
            }

            try {
                versionSetServiceClient.createVersionRevision(accountDetails.getId(), wsContext.getVersionSet(), newRevision);
            } catch (Exception e) {
                throw new ArchipelagoException("Was unable to create the new version-set revision", e);
            }

            buildService.setBuildStatus(buildId, BuildStage.PUBLISHING, BuildStatus.FINISHED);
        } catch (ArchipelagoException | IOException e) {
            log.error("Had an error while publishing builds to version-set", e);
            buildService.setBuildStatus(buildId, BuildStage.PUBLISHING, BuildStatus.FAILED);
            throw new PermanentMessageProcessingException(e);
        } finally {
            if (stageLog.hasLogs()) {
                buildService.uploadStageLog(buildId, BuildStage.PUBLISHING, stageLog.getLogs());
            }
        }
    }

    private boolean doseRevisionHaveChanges(List<ArchipelagoBuiltPackage> previous, List<ArchipelagoBuiltPackage> newRevision) {
        if (previous.size() != newRevision.size()) {
            return true;
        }

        Map<ArchipelagoBuiltPackage, Boolean> map = new HashMap<>();
        for(ArchipelagoBuiltPackage pkg : previous) {
            map.put(pkg, true);
        }
        for (ArchipelagoBuiltPackage pkg : newRevision) {
            if (!map.containsKey(pkg)) {
                return true;
            }
        }
        return false;
    }

    private ArchipelagoBuiltPackage getPreviousBuild(String name, String branch, String commit) {
        try {
            return packageServiceClient.getPackageByGit(accountDetails.getId(), name, branch, commit);
        } catch (PackageNotFoundException e) {
            log.debug("This is a new build of package \"{}\" for git branch \"{}\" commit \"{}\"",
                    name, branch, commit);
        }
        return null;
    }

    private Path prepareBuildZip(ArchipelagoPackage builtPackage) throws IOException {
        try {
            Path buildPath = wsContext.getPackageBuildPath(builtPackage);
            Path zipFolder = workspaceLocation.resolve("buildZips");
            if (!Files.exists(zipFolder)) {
                Files.createDirectory(zipFolder);
            }
            Path zipPath = workspaceLocation.resolve("buildZips").resolve(UUID.randomUUID().toString() + ".zip");
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
            return zipPath;
        } catch (Exception e) {
            throw new IOException("Unable to prepare the build zip for " + builtPackage.getNameVersion(), e);
        }
    }

    private void buildPackage(ArchipelagoPackage pkg) {
        MauiWrapper.ExecutionResult result = null;
        try {
            result = maui.executeWithWorkspaceCacheWithOutput(wsContext.getPackageRoot(pkg), "build");

            if (result.getExitCode() != 0) {
                failedBuild = true;
                buildService.setPackageStatus(buildId, pkg.getNameVersion(), BuildStatus.FAILED);
                buildService.setBuildStatus(buildId, BuildStage.PACKAGES, BuildStatus.FAILED);
            } else {
                buildService.setPackageStatus(buildId, pkg.getNameVersion(), BuildStatus.FINISHED);
            }
            buildService.uploadBuildLog(buildId, pkg.getNameVersion(), Files.readString(result.getOutputFile()));

            doneBuilding.put(pkg, true);
        } catch (Exception e) {
            log.error("Build failed because of a server error", e);
            failedBuild = true;
            buildService.setPackageStatus(buildId, pkg.getNameVersion(), BuildStatus.FAILED);
            buildService.uploadBuildLog(buildId, pkg.getNameVersion(), "Build server had a server error: " + e.getMessage());
        }
        finally {
            if (result != null) {
                result.clearFiles();
            }
        }
    }

    private synchronized ArchipelagoPackage getNextPackageToBuild() {
        // TODO: Detect where a dependency is not satisfied and never will
        while(buildQueue.size() > 0 && !failedBuild) {
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

    private void checkoutAffectedPackages(List<ArchipelagoPackage> getBuildPackages) throws IOException {
        List<ArchipelagoPackage> affectedPackages = findPackageAffectedByChange();
        for(ArchipelagoPackage pkg : affectedPackages.stream()
                .filter(p -> getBuildPackages.stream().noneMatch(bp -> bp.equals(p)))
                .collect(Collectors.toList())) {
            ArchipelagoBuiltPackage builtPackage = getBuiltPackageFromRevision(pkg);
            if (builtPackage == null) {
                throw new IOException("Affected package " + pkg + " was not in the version set revision");
            }
            GetPackageBuildResponse response;
            try {
                response = packageServiceClient.getPackageBuild(accountDetails.getId(), builtPackage);
            } catch (PackageNotFoundException e) {
                throw new IOException("The package " + builtPackage + " was not found on the package server", e);
            }

            checkOutPackage(builtPackage.getName(), response.getGitBranch(), response.getGitCommit());
        }
    }

    private ArchipelagoBuiltPackage getBuiltPackageFromRevision(ArchipelagoPackage pkg) throws IOException {
        try {
            Optional<ArchipelagoBuiltPackage> optional = wsContext.getVersionSetRevision().getPackages().stream()
                    .filter(b -> b.equals(pkg)).findFirst();
            if (optional.isPresent()) {
                return optional.get();
            }
        } catch (VersionSetNotSyncedException e) {
            throw new IOException("The version-set had not been synced");
        }
        return null;
    }

    private void checkOutPackagedToBuild() throws IOException {
        for (BuildPackageDetails details : request.getBuildPackages()) {
            checkOutPackage(details.getPackageName(), details.getBranch(), details.getCommit());
        }
    }

    private void checkOutPackage(String packageName, String branch, String commit) throws IOException {
        if (maui.execute(workspaceLocation, "ws", "use", "-p", packageName) != 0) {
            throw new IOException("Where unable to checkout the package " + packageName);
        }

        Path packagePath = findPackageDir(packageName);
        if (packagePath == null) {
            throw new IOException("Failed to find package directory for package: " + packageName);
        }

        if (executeProcess(packagePath, "git", "checkout", branch) != 0) {
            throw new IOException("Failed to checkout branch " + branch + " package " + packageName);
        }

        if (executeProcess(packagePath, "git", "checkout", commit) != 0) {
            throw new IOException("Failed to checkout commit " + commit + " package " + packageName);
        }
    }

    private int executeProcess(Path dir, String... args) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        processBuilder.directory(dir.toFile());
        processBuilder.command(args);
        log.debug("Running command in \"{}\": {}", dir, Strings.join(Arrays.asList(args), ' '));
        try {
            return processBuilder.start().waitFor();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private Path findPackageDir(String name) throws IOException {
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
        } catch (IOException e) {
            log.error("Was unable to find the package dir for " + name, e);
            throw e;
        }
        return packagePath;
    }

    private void generateGraphs() throws Exception {
        for (ArchipelagoPackage target : vs.getTargets()) {
            graphs.put(target, generateGraph(wsContext, target));
        }
    }

    private List<ArchipelagoPackage> findPackageAffectedByChange() {
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

    private ArchipelagoDependencyGraph generateGraph(WorkspaceContext wsContext, ArchipelagoPackage pkg) {
        try {
            return dependencyGraphGenerator.generateGraph(wsContext, pkg, DependencyTransversalType.ALL);
        } catch (Exception e) {
            String message = String.format("Error while generating graph for %s", pkg.getNameVersion());
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    private WorkspaceContext createWorkspaceContext() throws IOException {
        Path cachePath = workspaceLocation.resolve(".archipelago").resolve("cache");
        Path tempPath = workspaceLocation.resolve(".archipelago").resolve("temp");
        PackageCacher packageCacher = null;
        try {
            packageCacher = new LocalPackageCacher(cachePath, tempPath, harborClient);
        } catch (IOException e) {
            log.error("Failed to create the cache or temp dir");
            throw e;
        }
        WorkspaceContext ws = new WorkspaceContext(workspaceLocation, packageCacher);
        try {
            ws.load();
        } catch (IOException e) {
            log.error("Failed to load the workspace context");
            throw e;
        }
        return ws;
    }

    private void syncWorkspace() throws IOException {
        MauiWrapper.ExecutionResult result;
        try {
            int exitCode = maui.executeWithWorkspaceCache(workspaceLocation, "ws", "sync");
            if (exitCode != 0) {
                log.error("Got non zero return code ({}) when syncing the workspace", exitCode);
                throw new IOException("Failed to sync workspace");
            }
        } catch (IOException e) {
            log.error(String.format("Exception while syncing workspace \"%s\"", workspaceLocation), e);
            throw e;
        }
    }

    private void createWorkspace(String versionSet, String buildId) throws IOException {
        MauiWrapper.ExecutionResult result;
        try {
            int exitCode = maui.execute(buildLocation, "ws", "create", "-vs", versionSet, "--name", buildId);
            if (exitCode != 0) {
                log.error("Got non zero return code ({}) when syncing the workspace", exitCode);
                throw new IOException("Non-zero return code");
            }
        } catch (IOException e) {
            log.error("Exception while creating workspace", e);
            throw e;
        }
    }

    private Path createWorkspacePath(String buildId) {
        return buildLocation.resolve(buildId);
    }
}
