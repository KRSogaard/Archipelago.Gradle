package build.archipelago.buildserver.builder.builder;

import build.archipelago.account.common.AccountService;
import build.archipelago.account.common.exceptions.AccountNotFoundException;
import build.archipelago.account.common.exceptions.GitDetailsNotFound;
import build.archipelago.account.common.models.AccountDetails;
import build.archipelago.account.common.models.GitDetails;
import build.archipelago.buildserver.builder.StageLog;
import build.archipelago.buildserver.builder.clients.InternalHarborClientFactory;
import build.archipelago.buildserver.builder.git.GitHubPackageSourceProvider;
import build.archipelago.buildserver.builder.maui.Maui;
import build.archipelago.buildserver.common.services.build.BuildService;
import build.archipelago.buildserver.models.BuildStage;
import build.archipelago.buildserver.models.BuildStatus;
import build.archipelago.buildserver.common.services.build.exceptions.BuildRequestNotFoundException;
import build.archipelago.buildserver.models.rest.ArchipelagoBuild;
import build.archipelago.buildserver.models.BuildPackageDetails;
import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.concurrent.BlockingExecutorServiceFactory;
import build.archipelago.common.exceptions.ArchipelagoException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.common.exceptions.PackageNotLocalException;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.exceptions.VersionSetNotSyncedException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.common.PackageSourceProvider;
import build.archipelago.maui.common.WorkspaceConstants;
import build.archipelago.maui.common.contexts.WorkspaceContext;
import build.archipelago.maui.common.models.BuildConfig;
import build.archipelago.maui.core.output.ConsoleOutputWrapper;
import build.archipelago.maui.graph.ArchipelagoDependencyGraph;
import build.archipelago.maui.graph.ArchipelagoPackageEdge;
import build.archipelago.maui.graph.DependencyGraphGenerator;
import build.archipelago.maui.graph.DependencyTransversalType;
import build.archipelago.maui.path.MauiPath;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.GetPackageBuildResponse;
import build.archipelago.packageservice.client.models.UploadPackageRequest;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import com.wewelo.sqsconsumer.exceptions.PermanentMessageProcessingException;
import com.wewelo.sqsconsumer.exceptions.TemporaryMessageProcessingException;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.jgrapht.alg.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class VersionSetBuilder {
    private ExecutorService executorService;

    private InternalHarborClientFactory internalHarborClientFactory;
    private PackageServiceClient packageServiceClient;
    private VersionSetServiceClient versionSetServiceClient;
    private HarborClient harborClient;
    private Path buildLocation;
    private Path buildRoot;
    private BuildService buildService;
    private String buildId;
    private AccountService accountService;
    private DependencyGraphGenerator dependencyGraphGenerator;

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

    private Maui maui;
    private MauiPath mauiPath;
    private PackageSourceProvider packageSourceProvider;

    public VersionSetBuilder(InternalHarborClientFactory internalHarborClientFactory,
                             VersionSetServiceClient versionSetServiceClient,
                             PackageServiceClient packageServiceClient,
                             Path buildLocation,
                             BuildService buildService,
                             AccountService accountService,
                             MauiPath mauiPath,
                             String buildId) {
        this.internalHarborClientFactory = internalHarborClientFactory;
        this.versionSetServiceClient = versionSetServiceClient;
        this.packageServiceClient = packageServiceClient;
        this.buildLocation = buildLocation;
        this.buildService = buildService;
        this.buildId = buildId;
        this.accountService = accountService;
        this.mauiPath = mauiPath;

        graphs = new HashMap<>();
        buildQueue = new ConcurrentLinkedDeque<>();
        buildDependicies = new HashMap<>();
        doneBuilding = new HashMap<>();
        dependencyGraphGenerator = new DependencyGraphGenerator();
        executorService = new BlockingExecutorServiceFactory().create();
    }

    public void build() throws PermanentMessageProcessingException, TemporaryMessageProcessingException {
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

            GitDetails gitDetails;
            try {
                gitDetails = accountService.getGitDetails(request.getAccountId());
            } catch (GitDetailsNotFound gitDetailsNotFound) {
                throw new PermanentMessageProcessingException("No git settings was found for the account", gitDetailsNotFound);
            }
            buildRoot = createBuildRoot(buildId);
            if (!gitDetails.getCodeSource().toLowerCase().startsWith("github")) {
                log.error("Unknown code source {}", gitDetails.getCodeSource());
                throw new PermanentMessageProcessingException("Only github is supported at this time");
            }
            packageSourceProvider = new GitHubPackageSourceProvider(
                    gitDetails.getGithubAccount(),
                    gitDetails.getGitHubAccessToken());
            harborClient = internalHarborClientFactory.create(request.getAccountId());
            maui = new Maui(buildRoot, harborClient, packageSourceProvider, mauiPath);
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
                if (buildRoot != null && Files.exists(buildRoot) && Files.isDirectory(buildRoot)) {
                    try (Stream<Path> walk = Files.walk(buildRoot)) {
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

    private void stage_prepare() throws PermanentMessageProcessingException, TemporaryMessageProcessingException {
        StageLog stageLog = new StageLog();
        try {
            stageLog.addInfo("Build preparations started");
            buildService.setBuildStatus(buildId, BuildStage.PREPARE, BuildStatus.IN_PROGRESS);
            createWorkspace(request.getVersionSet());
            wsContext = maui.getWorkspaceContext();
            syncWorkspace();
            // We need to check out the packages before we create the graphs, the new build may have changed the graph
            // they may even have change the version number
            checkOutPackagedToBuild();
            generateGraphs();
            getBuildPackages = wsContext.getLocalArchipelagoPackages();

            for (ArchipelagoPackage pkg : wsContext.getLocalArchipelagoPackages()) {
                buildService.setPackageStatus(buildId, pkg.getNameVersion(), BuildStatus.WAITING);
            }

            checkoutAffectedPackages(getBuildPackages);
            // Recreate the workspace context with the newly checkout packages
            wsContext.load();
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
        } catch (TemporaryMessageProcessingException e) {
            throw e;
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
                        String configContent;
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

            List<ArchipelagoBuiltPackage> newRevision;
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
            Path zipFolder = buildRoot.resolve("zips");
            if (!Files.exists(zipFolder)) {
                Files.createDirectory(zipFolder);
            }
            Path zipPath = zipFolder.resolve(
                    builtPackage.getName() + "-" + builtPackage.getVersion() + "-" +
                            UUID.randomUUID().toString().substring(0, 6) + ".zip");
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
        if (!maui.build(new ConsoleOutputWrapper(), pkg)) {
            failedBuild = true;
            buildService.setPackageStatus(buildId, pkg.getNameVersion(), BuildStatus.FAILED);
            buildService.setBuildStatus(buildId, BuildStage.PACKAGES, BuildStatus.FAILED);
        } else {
            buildService.setPackageStatus(buildId, pkg.getNameVersion(), BuildStatus.FINISHED);
        }
        buildService.uploadBuildLog(buildId, pkg.getNameVersion(),"TODO, LOG GOES HERE");
        doneBuilding.put(pkg, true);
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

    private void checkoutAffectedPackages(List<ArchipelagoPackage> getBuildPackages) {
        List<ArchipelagoPackage> affectedPackages = findPackageAffectedByChange();
        for(ArchipelagoPackage pkg : affectedPackages.stream()
                .filter(p -> getBuildPackages.stream().noneMatch(bp -> bp.equals(p)))
                .collect(Collectors.toList())) {
            ArchipelagoBuiltPackage builtPackage = getBuiltPackageFromRevision(pkg);
            if (builtPackage == null) {
                throw new RuntimeException("Affected package " + pkg + " was not in the version set revision");
            }
            GetPackageBuildResponse response;
            try {
                response = packageServiceClient.getPackageBuild(accountDetails.getId(), builtPackage);
            } catch (PackageNotFoundException e) {
                throw new RuntimeException("The package " + builtPackage + " was not found on the package server", e);
            }

            checkOutPackage(builtPackage.getName(), response.getGitCommit());
        }
    }

    private ArchipelagoBuiltPackage getBuiltPackageFromRevision(ArchipelagoPackage pkg) {
        try {
            Optional<ArchipelagoBuiltPackage> optional = wsContext.getVersionSetRevision().getPackages().stream()
                    .filter(b -> b.equals(pkg)).findFirst();
            if (optional.isPresent()) {
                return optional.get();
            }
        } catch (VersionSetNotSyncedException e) {
            log.error("The workspace has not been synced");
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private void checkOutPackagedToBuild() {
        for (BuildPackageDetails details : request.getBuildPackages()) {
            checkOutPackage(details.getPackageName(), details.getCommit());
        }
    }

    private void checkOutPackage(String packageName, String commit) {
        packageSourceProvider.checkOutSource(maui.getWorkspaceLocation(), packageName, commit);
        wsContext.addLocalPackage(packageName);
        try {
            wsContext.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Path packagePath = findPackageDir(packageName);
        if (packagePath == null) {
            throw new RuntimeException("Failed to find package directory for package: " + packageName);
        }
    }

    private Path findPackageDir(String name) {
        Path packagePath = null;
        try {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(maui.getWorkspaceLocation())) {
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
            throw new RuntimeException(e);
        }
        return packagePath;
    }

    private void generateGraphs() {
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

    private void syncWorkspace() throws TemporaryMessageProcessingException {
        boolean successful = maui.syncWorkspace(new ConsoleOutputWrapper(), executorService);
        if (!successful) {
            log.error("Failed to sync the workspace");
            throw new TemporaryMessageProcessingException();
        }
    }

    private void createWorkspace(String versionSet) throws TemporaryMessageProcessingException {
        boolean successful = maui.createWorkspace(new ConsoleOutputWrapper(), versionSet);
        if (!successful) {
            log.error("Failed to create the workspace");
            throw new TemporaryMessageProcessingException();
        }
    }

    private Path createBuildRoot(String buildId) throws TemporaryMessageProcessingException {
        Path location = buildLocation.resolve(buildId);
        if (!Files.exists(location)) {
            try {
                Files.createDirectory(location);
            } catch (IOException e) {
                log.error("Failed to create build dir " + location);
                throw new TemporaryMessageProcessingException();
            }
        }
        return location;
    }
}
