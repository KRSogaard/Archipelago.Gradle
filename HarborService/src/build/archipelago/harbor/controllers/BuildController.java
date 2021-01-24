package build.archipelago.harbor.controllers;

import build.archipelago.buildserver.api.client.BuildServerAPIClient;
import build.archipelago.buildserver.models.*;
import build.archipelago.buildserver.models.client.Builds;
import build.archipelago.buildserver.models.exceptions.*;
import build.archipelago.buildserver.models.rest.*;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.harbor.filters.AccountIdFilter;
import build.archipelago.harbor.models.build.NewBuildRestResponse;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;
import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RequestMapping("builds")
@RestController
@Slf4j
@CrossOrigin(origins = "*")
public class BuildController {

    private BuildServerAPIClient buildServerAPIClient;

    public BuildController(BuildServerAPIClient buildServerAPIClient) {
        Preconditions.checkNotNull(buildServerAPIClient);
        this.buildServerAPIClient = buildServerAPIClient;
    }

    @GetMapping
    public BuildsRestResponse getBuilds(@RequestAttribute(AccountIdFilter.Key) String accountId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));

        Builds builds = buildServerAPIClient.getBuilds(accountId);

        return BuildsRestResponse.builder()
                .waitingBuilds(builds.getWaitingBuilds().stream()
                        .map(BuildRestResponse::from).collect(Collectors.toList()))
                .processingBuilds(builds.getProcessingBuilds().stream()
                        .map(BuildRestResponse::from).collect(Collectors.toList()))
                .pastBuilds(builds.getPastBuilds().stream()
                        .map(BuildRestResponse::from).collect(Collectors.toList()))
                .build();
    }

    @PostMapping
    public NewBuildRestResponse startBuild(@RequestAttribute(AccountIdFilter.Key) String accountId,
                                           @RequestBody NewBuildRestRequest request) throws VersionSetDoseNotExistsException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(request != null);
        request.validate();

        String buildId = buildServerAPIClient.startBuild(accountId,
                request.getVersionSet(),
                request.isDryRun(),
                request.getBuildPackages().stream().map(BuildPackageRestRequest::toInternal).collect(Collectors.toList()));

        return NewBuildRestResponse.builder()
                .buildId(buildId)
                .build();
    }

    @GetMapping("{buildId}")
    public BuildRestResponse getBuild(
            @RequestAttribute(AccountIdFilter.Key) String accountId,
            @PathVariable("buildId") String buildId) throws BuildNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));

        ArchipelagoBuild build = buildServerAPIClient.getBuild(accountId, buildId);
        return BuildRestResponse.from(build);
    }

    @GetMapping("{buildId}/packages")
    public BuildPackagesRestResponse getBuildPackages(
            @RequestAttribute(AccountIdFilter.Key) String accountId,
            @PathVariable("buildId") String buildId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));

        ImmutableList<PackageBuildStatus> packages = buildServerAPIClient.getBuildPackages(accountId, buildId);

        return BuildPackagesRestResponse.builder()
                .packages(packages.stream().map(BuildPackageStatusRestResponse::from).collect(Collectors.toList()))
                .build();
    }

    @GetMapping("{buildId}/log/stage/{stage}")
    public LogFileRestResponse getStageBuildLog(
            @RequestAttribute(AccountIdFilter.Key) String accountId,
            @PathVariable("buildId") String buildId,
            @PathVariable("stage") String stage) throws StageLogNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(stage));
        BuildStage buildStage = BuildStage.getEnum(stage);
        Preconditions.checkNotNull(BuildStage.getEnum(stage));

        LogFileResponse logFileResponse = buildServerAPIClient.getStageLog(accountId, buildId, buildStage);

        return LogFileRestResponse.builder()
                .signedUrl(logFileResponse.getSignedUrl())
                .build();
    }

    @GetMapping("{buildId}/log/package/{pkgNameVersion}")
    public LogFileRestResponse getPackageBuildLog(
            @RequestAttribute(AccountIdFilter.Key) String accountId,
            @PathVariable("buildId") String buildId,
            @PathVariable("pkgNameVersion") String pkgNameVersion) throws PackageLogNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(pkgNameVersion));
        ArchipelagoPackage pkg = ArchipelagoPackage.parse(pkgNameVersion);

        LogFileResponse logFileResponse = buildServerAPIClient.getPackageLog(accountId, buildId, pkg);

        return LogFileRestResponse.builder()
                .signedUrl(logFileResponse.getSignedUrl())
                .build();
    }
}
