package build.archipelago.buildserver.api.client.rest;

import build.archipelago.buildserver.api.client.*;
import build.archipelago.buildserver.models.*;
import build.archipelago.buildserver.models.client.Builds;
import build.archipelago.buildserver.models.exceptions.*;
import build.archipelago.buildserver.models.rest.*;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.clients.rest.OAuthRestClient;
import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.rest.models.errors.ProblemDetailRestResponse;
import build.archipelago.versionsetservice.client.VersionSetExceptionHandler;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;
import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;

import java.net.http.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RestBuildServerAPIClient extends OAuthRestClient implements BuildServerAPIClient {

    private static final String OAUTH2_SCOPES = "http://buildserver-api.archipelago.build/read http://buildserver-api.archipelago.build/create";
    private static final String OAUTH2_TOKENURL = "https://archipelago.auth.us-west-2.amazoncognito.com/oauth2/token";

    public RestBuildServerAPIClient(String endpoint, String clientId, String clientSecret) {
        super(endpoint, OAUTH2_TOKENURL, clientId, clientSecret, OAUTH2_SCOPES);
    }

    @Override
    public String startBuild(String accountId, String versionSet, boolean dryRun, List<BuildPackageDetails> packages) throws VersionSetDoseNotExistsException {
        HttpResponse<String> httpResponse;
        try {
            NewBuildRestRequest restRequest = NewBuildRestRequest.builder()
                    .versionSet(versionSet)
                    .dryRun(dryRun)
                    .buildPackages(packages.stream().map(BuildPackageRestRequest::from).collect(Collectors.toList()))
                    .build();

            HttpRequest request = this.getOAuthRequest("/account/" + accountId + "/build")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .build();
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call Build service to start a build for version set '{}' and account " +
                    "'{}'", versionSet, accountId);
            throw new RuntimeException(e);
        }

        switch (httpResponse.statusCode()) {
            case 200:
                return httpResponse.body();
            case 401:
            case 403:
                log.error("Got unauthorized response from Package service");
                throw new UnauthorizedException();
            case 404:
                log.warn("Got Not Found (404) response from Build service with body: " + httpResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (VersionSetDoseNotExistsException) VersionSetExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }
    }

    @Override
    public Builds getBuilds(String accountId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));

        BuildsRestResponse response;
        HttpResponse<String> httpResponse;
        try {
            HttpRequest request = this.getOAuthRequest("/account/" + accountId + "/build")
                    .GET()
                    .build();
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call Build service get builds for account '{}'", accountId);
            throw new RuntimeException(e);
        }
        switch (httpResponse.statusCode()) {
            case 200:
                response = this.parseOrThrow(httpResponse.body(), BuildsRestResponse.class);
                break;
            case 401:
            case 403:
                log.error("Got unauthorized response from Package service");
                throw new UnauthorizedException();
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }

        return Builds.builder()
                .waitingBuilds(response.getWaitingBuilds().stream()
                        .map(BuildRestResponse::toInternal).collect(Collectors.toList()))
                .processingBuilds(response.getProcessingBuilds().stream()
                        .map(BuildRestResponse::toInternal).collect(Collectors.toList()))
                .pastBuilds(response.getPastBuilds().stream()
                        .map(BuildRestResponse::toInternal).collect(Collectors.toList()))
                .build();
    }

    @Override
    public ArchipelagoBuild getBuild(String accountId, String buildId) throws BuildNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));

        HttpResponse<String> httpResponse;
        try {
            HttpRequest request = this.getOAuthRequest("/account/" + accountId + "/build/" + buildId)
                    .GET()
                    .build();
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call Build service get builds for account '{}'", accountId);
            throw new RuntimeException(e);
        }
        switch (httpResponse.statusCode()) {
            case 200:
                return this.parseOrThrow(httpResponse.body(), BuildRestResponse.class).toInternal();
            case 401:
            case 403:
                log.error("Got unauthorized response from Package service");
                throw new UnauthorizedException();
            case 404:
                log.warn("Got Not Found (404) response from Build service with body: " + httpResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (BuildNotFoundException) BuildsExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }
    }

    @Override
    public ImmutableList<PackageBuildStatus> getBuildPackages(String accountId, String buildId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));

        HttpResponse<String> httpResponse;
        try {
            HttpRequest request = this.getOAuthRequest("/account/" + accountId + "/build/" + buildId + "/packages")
                    .GET()
                    .build();
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call Build service get builds for account '{}'", accountId);
            throw new RuntimeException(e);
        }
        switch (httpResponse.statusCode()) {
            case 200:
                BuildPackagesRestResponse packages = this.parseOrThrow(httpResponse.body(), BuildPackagesRestResponse.class);
                return packages.getPackages().stream().map(BuildPackageStatusRestResponse::toInternal).collect(ImmutableList.toImmutableList());
            case 401:
            case 403:
                log.error("Got unauthorized response from Package service");
                throw new UnauthorizedException();
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }
    }

    @Override
    public LogFileResponse getStageLog(String accountId, String buildId, BuildStage stage) throws StageLogNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkNotNull(stage);

        HttpResponse<String> httpResponse;
        try {
            HttpRequest request = this.getOAuthRequest("/account/" + accountId + "/build/" + buildId + "/log/stage/" + stage.getStage())
                    .GET()
                    .build();
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call Build service get stage log for account '{}'", accountId);
            throw new RuntimeException(e);
        }
        switch (httpResponse.statusCode()) {
            case 200:
                LogFileRestResponse response = this.parseOrThrow(httpResponse.body(), LogFileRestResponse.class);
                return LogFileResponse.builder().signedUrl(response.getSignedUrl()).build();
            case 401:
            case 403:
                log.error("Got unauthorized response from Package service");
                throw new UnauthorizedException();
            case 404:
                log.warn("Got Not Found (404) response from Build service with body: " + httpResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (StageLogNotFoundException) BuildsExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }
    }

    @Override
    public LogFileResponse getPackageLog(String accountId, String buildId, ArchipelagoPackage pkg) throws PackageLogNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkNotNull(pkg);

        HttpResponse<String> httpResponse;
        try {
            HttpRequest request = this.getOAuthRequest(
                    "/account/" + accountId + "/build/" + buildId + "/log/package/" + pkg.getNameVersion())
                    .GET()
                    .build();
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call Build service get stage log for account '{}'", accountId);
            throw new RuntimeException(e);
        }
        switch (httpResponse.statusCode()) {
            case 200:
                LogFileRestResponse response = this.parseOrThrow(httpResponse.body(), LogFileRestResponse.class);
                return LogFileResponse.builder().signedUrl(response.getSignedUrl()).build();
            case 401:
            case 403:
                log.error("Got unauthorized response from Package service");
                throw new UnauthorizedException();
            case 404:
                log.warn("Got Not Found (404) response from Build service with body: " + httpResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (PackageLogNotFoundException) BuildsExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }
    }

    private RuntimeException logAndReturnExceptionForUnknownStatusCode(HttpResponse<String> restResponse) {
        log.error("Unknown response from Build service status code " + restResponse.statusCode() +
                " body: " + restResponse.body());
        return new RuntimeException("Unknown response from Build service status code " + restResponse.statusCode() +
                " body: " + restResponse.body());
    }
}
