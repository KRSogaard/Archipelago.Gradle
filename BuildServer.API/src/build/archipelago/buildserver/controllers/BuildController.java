package build.archipelago.buildserver.controllers;

import build.archipelago.buildserver.common.services.build.BuildService;
import build.archipelago.buildserver.common.services.build.models.BuildPackageDetails;
import build.archipelago.buildserver.models.NewBuildRequest;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("account/{accountId}/build")
@Slf4j
public class BuildController {

    private VersionSetServiceClient versionSetServiceClient;
    private PackageServiceClient packageServiceClient;
    private BuildService buildService;

    @PostMapping("request")
    @ResponseStatus(HttpStatus.OK)
    public String newBuildRequest(
            @PathVariable("accountId") String accountId,
            @RequestBody NewBuildRequest request) throws VersionSetDoseNotExistsException {
        VersionSet versionSet = versionSetServiceClient.getVersionSet(accountId, request.getVersionSet());

        String buildId = buildService.addNewBuildRequest(accountId, versionSet.getName(), request.isDryRun(),
                request.getBuildPackages().stream().map(bp -> BuildPackageDetails.builder()
                        .packageName(bp.getPackageName())
                        .branch(bp.getBranch())
                        .commit(bp.getCommit())
                        .build())
                        .collect(Collectors.toList()));
        return buildId;
    }
}
