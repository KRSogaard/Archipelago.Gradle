package build.archipelago.harbor.controllers;

import build.archipelago.buildserver.api.client.BuildServerAPIClient;
import build.archipelago.buildserver.models.client.Builds;
import build.archipelago.buildserver.models.rest.*;
import build.archipelago.harbor.filters.AccountIdFilter;
import build.archipelago.harbor.models.build.NewBuildRestResponse;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;
import com.google.common.base.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.OK)
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
}
