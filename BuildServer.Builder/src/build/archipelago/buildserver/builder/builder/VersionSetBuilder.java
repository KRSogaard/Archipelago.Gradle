package build.archipelago.buildserver.builder.builder;

import build.archipelago.account.common.AccountService;
import build.archipelago.account.common.exceptions.*;
import build.archipelago.account.common.models.*;
import build.archipelago.buildserver.builder.StageLog;
import build.archipelago.buildserver.builder.builder.helpers.*;
import build.archipelago.buildserver.builder.clients.InternalHarborClientFactory;
import build.archipelago.buildserver.builder.git.GitServiceSourceProvider;
import build.archipelago.buildserver.builder.maui.Maui;
import build.archipelago.buildserver.builder.output.*;
import build.archipelago.buildserver.common.services.build.DynamoDBBuildService;
import build.archipelago.buildserver.common.services.build.logs.StageLogsService;
import build.archipelago.buildserver.common.services.build.models.BuildQueueMessage;
import build.archipelago.buildserver.models.*;
import build.archipelago.buildserver.models.exceptions.BuildNotFoundException;
import build.archipelago.common.*;
import build.archipelago.common.concurrent.BlockingExecutorServiceFactory;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.github.*;
import build.archipelago.common.github.exceptions.RepoNotFoundException;
import build.archipelago.common.versionset.*;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.common.*;
import build.archipelago.maui.common.contexts.WorkspaceContext;
import build.archipelago.maui.common.serializer.VersionSetRevisionSerializer;
import build.archipelago.maui.core.output.ConsoleOutputWrapper;
import build.archipelago.maui.path.MauiPath;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.UploadPackageRequest;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.models.*;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;
import com.wewelo.sqsconsumer.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.*;

@Slf4j
public class VersionSetBuilder {
    // Only used once
    private Path buildLocation;

    private ExecutorService executorService;

    private InternalHarborClientFactory internalHarborClientFactory;
    private PackageServiceClient packageServiceClient;
    private VersionSetServiceClient versionSetServiceClient;
    private HarborClient harborClient;
    private Path buildRoot;
    private StageLogsService stageLogsService;
    private DynamoDBBuildService buildService;
    private BuildQueueMessage buildRequest;
    private AccountService accountService;
    private GitServiceFactory gitServiceFactory;
    private PackageSourceProvider packageSourceProvider;
    private S3OutputWrapperFactory s3OutputWrapperFactory;

    private ArchipelagoBuild request;
    private AccountDetails accountDetails;
    private WorkspaceContext wsContext;
    private BuildQueue buildQueue;

    private List<ArchipelagoPackage> directPackages;
    private boolean failedBuild = false;

    private Maui maui;
    private MauiPath mauiPath;
    private GitService gitService;

    public VersionSetBuilder(InternalHarborClientFactory internalHarborClientFactory,
                             VersionSetServiceClient versionSetServiceClient,
                             PackageServiceClient packageServiceClient,
                             Path buildLocation,
                             GitServiceFactory gitServiceFactory,
                             S3OutputWrapperFactory s3OutputWrapperFactory,
                             StageLogsService stageLogsService,
                             DynamoDBBuildService buildService,
                             AccountService accountService,
                             MauiPath mauiPath,
                             BuildQueueMessage buildRequest) {
        this.internalHarborClientFactory = internalHarborClientFactory;
        this.versionSetServiceClient = versionSetServiceClient;
        this.packageServiceClient = packageServiceClient;
        this.buildLocation = buildLocation;
        this.buildService = buildService;
        this.buildRequest = buildRequest;
        this.accountService = accountService;
        this.mauiPath = mauiPath;
        this.gitServiceFactory = gitServiceFactory;
        this.s3OutputWrapperFactory = s3OutputWrapperFactory;
        this.stageLogsService = stageLogsService;

        executorService = new BlockingExecutorServiceFactory().create();
    }

    public void build() throws PermanentMessageProcessingException, TemporaryMessageProcessingException {
        try {
            try {
                request = buildService.getBuildRequest(buildRequest.getAccountId(), buildRequest.getBuildId());
            } catch (BuildNotFoundException e) {
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

            buildRoot = PathHelper.createBuildRoot(buildLocation,
                    buildRequest.getAccountId() + "-" + Instant.now().toEpochMilli() + "-" + buildRequest.getBuildId());
            if (!gitDetails.getCodeSource().toLowerCase().startsWith("github")) {
                log.error("Unknown code source {}", gitDetails.getCodeSource());
                throw new PermanentMessageProcessingException("Only github is supported at this time");
            }
            gitService = gitServiceFactory.getGitService(gitDetails.getCodeSource(), gitDetails.getGithubAccount(), gitDetails.getGitHubAccessToken());
            packageSourceProvider = new GitServiceSourceProvider(gitService);
            harborClient = internalHarborClientFactory.create(request.getAccountId());
            maui = new Maui(buildRoot, harborClient, packageSourceProvider, mauiPath);

            buildService.setBuildStatus(buildRequest.getAccountId(), buildRequest.getBuildId(), BuildStage.PREPARE, BuildStatus.WAITING);
            buildService.setBuildStatus(buildRequest.getAccountId(), buildRequest.getBuildId(), BuildStage.PACKAGES, BuildStatus.WAITING);
            buildService.setBuildStatus(buildRequest.getAccountId(), buildRequest.getBuildId(), BuildStage.PUBLISHING, BuildStatus.WAITING);

            this.stage_prepare();
            this.stage_packages();
            this.stage_publish();
        } catch (FailBuildException exp) {
            log.warn("The build was failed, will not retry", exp);
        } catch (RuntimeException exp) {
            log.error("Fatal error while processing build, will retry later", exp);
            throw new TemporaryMessageProcessingException();
        } finally {
            //PathHelper.deleteFolder(buildRoot);
        }
    }

    private void stage_prepare() {
        StageLog stageLog = new StageLog();
        try {
            stageLog.addInfo("Build preparations started");
            buildService.setBuildStatus(buildRequest.getAccountId(), buildRequest.getBuildId(), BuildStage.PREPARE, BuildStatus.IN_PROGRESS);
            this.createWorkspace(request.getVersionSet());
            wsContext = maui.getWorkspaceContext();
            this.syncWorkspace();
            this.checkOutPackagedToBuild();
            directPackages = wsContext.getLocalArchipelagoPackages();

            this.checkoutAffectedPackages(directPackages);
            wsContext.load();

            this.setBuildPackages(directPackages, wsContext.getLocalArchipelagoPackages());

            buildQueue = new BuildQueue(wsContext);

            buildService.setBuildStatus(buildRequest.getAccountId(), buildRequest.getBuildId(), BuildStage.PREPARE, BuildStatus.FINISHED);
            stageLog.addInfo("Build preparations finished");
        } catch (PackageNotFoundException exp) {
            stageLog.addError("The package %s was not found.", exp.getPackageName());
            log.error("Was unable to find the package {}, can not continue.", exp.getPackageName());
            throw new FailBuildException();
        } catch (RepoNotFoundException e) {
            stageLog.addError("The repo " + e.getRepo() + " was not found");
            log.error("The repo " + e.getRepo() + " was not found");
            throw new FailBuildException();
        } catch (Exception e) {
            log.error("The prepare stage failed with an exception", e);
            stageLog.addError("An unknown error occurred, need to restart the build: %s", e.getMessage());
            buildService.setBuildStatus(buildRequest.getAccountId(), buildRequest.getBuildId(), BuildStage.PREPARE, BuildStatus.FAILED);
            throw new RuntimeException(e);
        } finally {
            if (stageLog.hasLogs()) {
                stageLogsService.uploadStageLog(buildRequest.getBuildId(), BuildStage.PREPARE, stageLog.getLogs());
            }
        }
    }

    private void setBuildPackages(List<ArchipelagoPackage> directPackages, List<ArchipelagoPackage> localArchipelagoPackages) {
        Map<ArchipelagoPackage, Boolean> directMap = new HashMap();
        for (ArchipelagoPackage pkg : directPackages) {
            directMap.put(pkg, true);
        }
        List<PackageBuild> pkgs = new ArrayList<>();
        for (ArchipelagoPackage pkg : localArchipelagoPackages) {
            pkgs.add(PackageBuild.builder()
                    .pkg(pkg)
                    .direct(directMap.containsKey(pkg))
                    .build());
        }
        buildService.setBuildPackages(buildRequest.getBuildId(), pkgs);
    }

    private void stage_packages() {
        StageLog stageLog = new StageLog();
        stageLog.addInfo("Starting package builds");
        try {
            buildService.setBuildStatus(buildRequest.getAccountId(), buildRequest.getBuildId(), BuildStage.PACKAGES, BuildStatus.IN_PROGRESS);
            ArchipelagoPackage pkg = buildQueue.getNext();
            while (pkg != null) {
                if (failedBuild) {
                    break;
                }
                log.info("Building " + pkg);
                stageLog.addInfo("Starting build of %s", pkg.toString());
                this.buildPackage(pkg);
                stageLog.addInfo("Finished build of %s", pkg.toString());
                pkg = buildQueue.getNext();
            }
            // Currently the build Package is in thread, but later it will be multi threaded,
            // therefore we may get to here even though we failed the build.
            if (!failedBuild) {
                stageLog.addInfo("Finished building packages");
                buildService.setBuildStatus(buildRequest.getAccountId(), buildRequest.getBuildId(), BuildStage.PACKAGES, BuildStatus.FINISHED);
            } else {
                log.debug("One of the packages failed it's build, failing the whole build");
                throw new FailBuildException();
            }
        } finally {
            if (failedBuild) {
                stageLog.addInfo("Package build failed");
                buildService.setBuildStatus(buildRequest.getAccountId(), buildRequest.getBuildId(), BuildStage.PACKAGES, BuildStatus.FAILED);
            }
            if (stageLog.hasLogs()) {
                stageLogsService.uploadStageLog(buildRequest.getBuildId(), BuildStage.PACKAGES, stageLog.getLogs());
            }
        }
    }

    private void stage_publish() {
        if (request.isDryRun()) {
            buildService.setBuildStatus(buildRequest.getAccountId(), buildRequest.getBuildId(), BuildStage.PUBLISHING, BuildStatus.SKIPPED);
            return;
        }
        StageLog stageLog = new StageLog();
        try {
            buildService.setBuildStatus(buildRequest.getAccountId(), buildRequest.getBuildId(), BuildStage.PUBLISHING, BuildStatus.IN_PROGRESS);
            stageLog.addInfo("Starting version set publishing");

            Map<ArchipelagoPackage, String> gitMap = new HashMap<>();
            for (BuildPackageDetails bpd : request.getBuildPackages()) {
                Optional<ArchipelagoPackage> archipelagoPackage = directPackages.stream()
                        .filter(p -> p.getName().equalsIgnoreCase(bpd.getPackageName()))
                        .findFirst();
                if (archipelagoPackage.isEmpty()) {
                    throw new RuntimeException(String.format("Could not find the archipelago package \"%s\" in build details", bpd.getPackageName()));
                }
                gitMap.put(archipelagoPackage.get(), bpd.getCommit());
            }

            // Build are done
            List<ArchipelagoBuiltPackage> newBuildPackage = new ArrayList<>();
            for (ArchipelagoPackage builtPackage : directPackages) {
                if (!gitMap.containsKey(builtPackage)) {
                    log.error("Build package {} was not in the git map", builtPackage);
                    throw new RuntimeException(String.format("Build package %s was not in the git map", builtPackage));
                }
                String gitCommit = gitMap.get(builtPackage);
                String buildHash = null;

                try {
                    ArchipelagoBuiltPackage previousBuilt = this.getPreviousBuild(builtPackage.getName(), gitCommit);
                    if (previousBuilt != null) {
                        buildHash = previousBuilt.getHash();
                    }

                    if (buildHash == null) {
                        log.info("First time the package {}, at commit {} has been built",
                                builtPackage.getName(), gitCommit);
                        try {
                            String configContent;
                            try {
                                configContent = Files.readString(wsContext.getPackageRoot(builtPackage).resolve(WorkspaceConstants.BUILD_FILE_NAME));
                            } catch (PackageNotLocalException e) {
                                throw new RuntimeException("Where not able to find the package dir for " + builtPackage, e);
                            }

                            Path zip = this.prepareBuildZip(builtPackage);
                            buildHash = packageServiceClient.uploadBuiltArtifact(accountDetails.getId(), UploadPackageRequest.builder()
                                            .config(configContent)
                                            .pkg(builtPackage)
                                            .gitCommit(gitCommit)
                                            .build(),
                                    zip);
                        } catch (PackageNotFoundException e) {
                            throw new RuntimeException(String.format("The package %s no longer exists, was it deleted while building?", builtPackage), e);
                        }
                    }
                } catch (Exception exp) {
                    log.error("Failed to get last build of package", exp);
                    log.error("Failed to get last build of package", exp);
                }

                newBuildPackage.add(new ArchipelagoBuiltPackage(builtPackage, buildHash));
            }

            List<ArchipelagoBuiltPackage> newRevision;
            try {
                newRevision = wsContext.getVersionSetRevision().getPackages()
                        .stream().filter(rp -> newBuildPackage.stream().noneMatch(p -> rp.getNameVersion().equalsIgnoreCase(p.getNameVersion())))
                        .collect(Collectors.toList());
            } catch (VersionSetNotSyncedException e) {
                throw new RuntimeException("The workspace had not been synced", e);
            }
            newRevision.addAll(newBuildPackage);

            if (!this.doseRevisionHaveChanges(wsContext.getVersionSetRevision().getPackages(), newRevision)) {
                log.warn("There are no changes to the version set");
                stageLog.addError("There are no change to the version set, can't publish");
                throw new FailBuildException();
            }

            try {
                String revision = versionSetServiceClient.createVersionRevision(accountDetails.getId(), wsContext.getVersionSet(), newRevision);
                stageLog.addInfo("Revision %s was created for version set %s", revision, wsContext.getVersionSet());
            } catch (Exception e) {
                throw new RuntimeException("Was unable to create the new version-set revision", e);
            }

            buildService.setBuildStatus(buildRequest.getAccountId(), buildRequest.getBuildId(), BuildStage.PUBLISHING, BuildStatus.FINISHED);
        } catch (FailBuildException exp) {
            throw exp;
        } catch (Exception e) {
            log.error("Had an error while publishing builds to version-set", e);
            buildService.setBuildStatus(buildRequest.getAccountId(), buildRequest.getBuildId(), BuildStage.PUBLISHING, BuildStatus.FAILED);
            throw new RuntimeException(e);
        } finally {
            if (stageLog.hasLogs()) {
                stageLogsService.uploadStageLog(buildRequest.getBuildId(), BuildStage.PUBLISHING, stageLog.getLogs());
            }
        }
    }

    private boolean doseRevisionHaveChanges(List<ArchipelagoBuiltPackage> previous, List<ArchipelagoBuiltPackage> newRevision) {
        if (previous.size() != newRevision.size()) {
            return true;
        }

        Map<ArchipelagoBuiltPackage, Boolean> map = new HashMap<>();
        for (ArchipelagoBuiltPackage pkg : previous) {
            map.put(pkg, true);
        }
        for (ArchipelagoBuiltPackage pkg : newRevision) {
            if (!map.containsKey(pkg)) {
                return true;
            }
        }
        return false;
    }

    private ArchipelagoBuiltPackage getPreviousBuild(String name, String commit) {
        try {
            return packageServiceClient.getPackageByGit(accountDetails.getId(), name, commit);
        } catch (PackageNotFoundException e) {
            log.debug("This is a new build of package '{}' for git commit '{}'",
                    name, commit);
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
            ZipFile zip = new ZipFile(zipPath.toFile());
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
        S3OutputWrapper outputWrapper = s3OutputWrapperFactory.create(request.getAccountId(), request.getBuildId(), pkg);
        try {
            buildService.setPackageStatus(buildRequest.getBuildId(), pkg, BuildStatus.IN_PROGRESS);
            if (!maui.build(outputWrapper, pkg)) {
                failedBuild = true;
                buildService.setPackageStatus(buildRequest.getBuildId(), pkg, BuildStatus.FAILED);
                buildService.setBuildStatus(buildRequest.getAccountId(), buildRequest.getBuildId(), BuildStage.PACKAGES, BuildStatus.FAILED);
                throw new FailBuildException();
            } else {
                buildService.setPackageStatus(buildRequest.getBuildId(), pkg, BuildStatus.FINISHED);
            }
        } finally {
            outputWrapper.upload();
        }
        buildQueue.setPackageBuilt(pkg);
    }

    private void checkoutAffectedPackages(List<ArchipelagoPackage> buildPackages)
            throws PackageNotFoundException, RepoNotFoundException {
        List<ArchipelagoPackage> affectedPackages = AffectedPackagesHelper.findAffectedPackages(wsContext, buildPackages);

        for (ArchipelagoPackage pkg : affectedPackages.stream()
                .filter(p -> buildPackages.stream().noneMatch(bp -> bp.equals(p)))
                .collect(Collectors.toList())) {
            ArchipelagoBuiltPackage builtPackage = this.getBuiltPackageFromRevision(pkg);
            if (builtPackage == null) {
                throw new RuntimeException("Affected package " + pkg + " was not in the version set revision");
            }

            PackageDetails packageDetails;
            BuiltPackageDetails response;

            packageDetails = packageServiceClient.getPackage(
                    accountDetails.getId(), builtPackage.getName());
            response = packageServiceClient.getPackageBuild(accountDetails.getId(), builtPackage);

            this.checkOutPackage(packageDetails, response.getGitCommit());
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

    private void checkOutPackagedToBuild() throws PackageNotFoundException, RepoNotFoundException {
        for (BuildPackageDetails pkg : request.getBuildPackages()) {
            PackageDetails packageDetails = packageServiceClient.getPackage(request.getAccountId(), pkg.getPackageName());
            this.checkOutPackage(packageDetails, pkg.getCommit());
        }
    }

    private void checkOutPackage(PackageDetails packageDetails, String commit) throws RepoNotFoundException {
        packageSourceProvider.checkOutSource(maui.getWorkspaceLocation(), packageDetails, commit);
        wsContext.addLocalPackage(packageDetails.getName());
        try {
            wsContext.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Path packagePath = PathHelper.findPackageDir(maui.getWorkspaceLocation(), packageDetails.getName());
        if (packagePath == null) {
            throw new RuntimeException("Failed to find package directory for package: " + packageDetails.getName());
        }
    }

    private void syncWorkspace() {
        try {
            VersionSet vs = versionSetServiceClient.getVersionSet(accountDetails.getId(), wsContext.getVersionSet());
            if (vs.getLatestRevision() == null) {
                log.warn("Version-set {} did not have a latest build, it must be new, creating an empty sync", wsContext.getVersionSet());
                VersionSetRevisionSerializer.save(VersionSetRevision.builder()
                        .created(Instant.now())
                        .packages(new ArrayList<>())
                        .build(), maui.getWorkspaceLocation());
                return;
            }
        } catch (VersionSetDoseNotExistsException e) {
            log.error("The version-set did not exists, can't sync", e);
            throw new FailBuildException();
        } catch (IOException e) {
            log.error("Was unable to create local version-set revision", e);
        }
        boolean successful = maui.syncWorkspace(new ConsoleOutputWrapper(), executorService);
        if (!successful) {
            log.error("Failed to sync the workspace");
            throw new RuntimeException("Was unable to sync the workspace");
        }
    }

    private void createWorkspace(String versionSet) throws TemporaryMessageProcessingException {
        boolean successful = maui.createWorkspace(new ConsoleOutputWrapper(), versionSet);
        if (!successful) {
            log.error("Failed to create the workspace");
            throw new TemporaryMessageProcessingException();
        }
    }
}
