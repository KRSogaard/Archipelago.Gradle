package build.archipelago.buildserver.controllers;

import build.archipelago.buildserver.common.services.build.BuildService;
import build.archipelago.buildserver.common.services.build.logs.*;
import build.archipelago.buildserver.models.*;
import build.archipelago.buildserver.models.exceptions.*;
import build.archipelago.buildserver.models.rest.*;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;
import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("account/{accountId}/build")
@Slf4j
public class BuildController {

    private VersionSetServiceClient versionSetServiceClient;
    private PackageServiceClient packageServiceClient;
    private StageLogsService stageLogsService;
    private BuildService buildService;
    private PackageLogsService packageLogsService;

    public BuildController(VersionSetServiceClient versionSetServiceClient,
                           PackageServiceClient packageServiceClient,
                           BuildService buildService,
                           StageLogsService stageLogsService,
                           PackageLogsService packageLogsService) {
        this.versionSetServiceClient = versionSetServiceClient;
        this.packageServiceClient = packageServiceClient;
        this.buildService = buildService;
        this.stageLogsService = stageLogsService;
        this.packageLogsService = packageLogsService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public String newBuildRequest(
            @PathVariable("accountId") String accountId,
            @RequestBody NewBuildRestRequest request) throws VersionSetDoseNotExistsException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(request != null);
        request.validate();

        VersionSet versionSet = versionSetServiceClient.getVersionSet(accountId, request.getVersionSet());

        String buildId = buildService.addNewBuildRequest(accountId, versionSet.getName(), request.isDryRun(),
                request.getBuildPackages().stream().map(BuildPackageRestRequest::toInternal)
                        .collect(Collectors.toList()));
        return buildId;
    }


    @GetMapping
    public BuildsRestResponse getCurrentBuilds(
            @PathVariable("accountId") String accountId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));

        List<ArchipelagoBuild> builds = buildService.getAllBuildsForAccount(accountId);
        List<BuildRestResponse> processingBuilds = new ArrayList<>();
        List<BuildRestResponse> waitingBuilds = new ArrayList<>();
        List<BuildRestResponse> pastBuilds = new ArrayList<>();
        for (ArchipelagoBuild build : builds) {
            BuildRestResponse restBuild = BuildRestResponse.from(build);
            switch (build.getBuildStatus()) {
                case IN_PROGRESS:
                    processingBuilds.add(restBuild);
                    break;
                case WAITING:
                    waitingBuilds.add(restBuild);
                    break;
                case FAILED:
                case FINISHED:
                    pastBuilds.add(restBuild);
                    break;
                default:
                    log.error("Build has unknown status {}", build.getBuildStatus());
                    throw new RuntimeException("Unknown build status " + build.getBuildStatus());
            }
        }

        return BuildsRestResponse.builder()
                .waitingBuilds(waitingBuilds)
                .processingBuilds(processingBuilds)
                .pastBuilds(pastBuilds)
                .build();
    }

    @GetMapping("{buildId}")
    public BuildRestResponse getBuild(
            @PathVariable("accountId") String accountId,
            @PathVariable("buildId") String buildId) throws BuildNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));

        ArchipelagoBuild build = buildService.getBuildRequest(accountId, buildId);
        return BuildRestResponse.from(build);
    }

    @GetMapping("{buildId}/packages")
    public BuildPackagesRestResponse getBuildPackages(
            @PathVariable("accountId") String accountId,
            @PathVariable("buildId") String buildId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));

        ImmutableList<PackageBuildStatus> packages = buildService.getBuildPackages(accountId, buildId);

        return BuildPackagesRestResponse.builder()
                .packages(packages.stream().map(BuildPackageStatusRestResponse::from).collect(Collectors.toList()))
                .build();
    }

    @GetMapping("{buildId}/log/stage/{stage}")
    public LogFileRestResponse getStageLog(
            @PathVariable("accountId") String accountId,
            @PathVariable("buildId") String buildId,
            @PathVariable("stage") String stage) throws StageLogNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(stage));
        BuildStage buildStage = BuildStage.getEnum(stage);
        Preconditions.checkNotNull(BuildStage.getEnum(stage));

        String signedUrl = stageLogsService.getStageBuildLog(accountId, buildId, buildStage);
        return LogFileRestResponse.builder()
                .signedUrl(signedUrl)
                .build();
    }

    @GetMapping("{buildId}/log/package/{pkgNameVersion}")
    public LogFileRestResponse getPackageLog(
            @PathVariable("accountId") String accountId,
            @PathVariable("buildId") String buildId,
            @PathVariable("pkgNameVersion") String pkgName) throws PackageLogNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(pkgName));
        ArchipelagoPackage pkg = ArchipelagoPackage.parse(pkgName);

        String signedUrl = packageLogsService.getPackageBuildLog(accountId, buildId, pkg);
        return LogFileRestResponse.builder()
                .signedUrl(signedUrl)
                .build();
    }
}
