package build.archipelago.buildserver.controllers;

import build.archipelago.buildserver.common.services.build.BuildService;
import build.archipelago.buildserver.models.ArchipelagoBuild;
import build.archipelago.buildserver.models.rest.*;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;
import com.google.common.base.*;
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
    private BuildService buildService;

    public BuildController(VersionSetServiceClient versionSetServiceClient,
                           PackageServiceClient packageServiceClient, BuildService buildService) {
        this.versionSetServiceClient = versionSetServiceClient;
        this.packageServiceClient = packageServiceClient;
        this.buildService = buildService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public String newBuildRequest(
            @PathVariable("accountId") String accountId,
            @RequestBody NewBuildRestRequest request) throws VersionSetDoseNotExistsException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
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
}
