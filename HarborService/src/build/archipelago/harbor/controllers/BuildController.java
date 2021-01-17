package build.archipelago.harbor.controllers;

import build.archipelago.buildserver.api.client.BuildServerAPIClient;
import build.archipelago.buildserver.models.client.Builds;
import build.archipelago.buildserver.models.rest.BuildPackageRestRequest;
import build.archipelago.buildserver.models.rest.BuildRestResponse;
import build.archipelago.buildserver.models.rest.BuildsRestResponse;
import build.archipelago.buildserver.models.rest.NewBuildRestRequest;
import build.archipelago.harbor.filters.AccountIdFilter;
import build.archipelago.harbor.models.build.NewBuildRestResponse;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
                                           @RequestBody NewBuildRestRequest request) {
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
