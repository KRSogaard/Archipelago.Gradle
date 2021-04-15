package build.archipelago.versionsetservice.client.rest;

import build.archipelago.common.*;
import build.archipelago.common.clients.rest.OAuthRestClient;
import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.rest.models.errors.ProblemDetailRestResponse;
import build.archipelago.common.utils.O;
import build.archipelago.common.versionset.*;
import build.archipelago.packageservice.client.PackageExceptionHandler;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.versionsetservice.client.*;
import build.archipelago.versionsetservice.exceptions.*;
import build.archipelago.versionsetservice.models.*;
import build.archipelago.versionsetservice.models.rest.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.*;
import lombok.extern.slf4j.Slf4j;

import java.net.http.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RestVersionSetServiceClient extends OAuthRestClient implements VersionSetServiceClient {

    private static final String OAUTH2_SCOPES = "http://versionsetservice.archipelago.build/write http://versionsetservice.archipelago.build/read";
    private static final String OAUTH2_TOKENURL = "https://archipelago.auth.us-west-2.amazoncognito.com/oauth2/token";

    public RestVersionSetServiceClient(String endpoint, String clientId, String clientSecret) {
        super(endpoint, OAUTH2_TOKENURL, clientId, clientSecret, OAUTH2_SCOPES);
    }

    @Override
    public List<VersionSet> getVersionSets(String accountId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");

        VersionSetsRestResponse response;
        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/account/" + accountId + "/version-set")
                    .GET()
                    .build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call version set service to get a list of version sets for account '{}'", accountId);
            throw new RuntimeException(e);
        }
        switch (httpResponse.statusCode()) {
            case 200: // Ok
                response = this.parseOrThrow(httpResponse.body(), VersionSetsRestResponse.class);
                break;
            case 401:
            case 403:
                log.error("Got unauthorized response from Version set service");
                throw new UnauthorizedException();
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }

        return response.getVersionSets().stream()
                .map(VersionSetRestResponse::toInternal)
                .collect(Collectors.toList());
    }

    @Override
    public void createVersionSet(String accountId, CreateVersionSetRequest request)
            throws VersionSetDoseNotExistsException, VersionSetExistsException, PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkNotNull(request);
        request.validate();

        CreateVersionSetRestRequest restRequest = CreateVersionSetRestRequest.builder()
                .name(request.getName())
                .target(O.getOrNull(request.getTarget(), ArchipelagoPackage::getNameVersion))
                .parent(O.getOrNull(request.getParent()))
                .build();

        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/account/" + accountId + "/version-set")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call version set service to create a version set for account '{}'", accountId);
            throw new RuntimeException(e);
        }
        ProblemDetailRestResponse problem;
        switch (httpResponse.statusCode()) {
            case 200:
                break;
            case 401:
            case 403:
                log.error("Got unauthorized response from Version set service");
                throw new UnauthorizedException();
            case 409:
                log.warn("Got Conflict (409) response from Version set service with body: " + httpResponse.body());
                problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (VersionSetExistsException) VersionSetExceptionHandler.createException(problem);
            case 404:
                log.warn("Got Not Found (404) response from Version set service with body: " + httpResponse.body());
                problem = ProblemDetailRestResponse.from(httpResponse.body());
                switch (problem.getError()) {
                    case VersionSetExceptionHandler.TYPE_VERSION_SET_NOT_FOUND:
                        throw (VersionSetDoseNotExistsException) VersionSetExceptionHandler.createException(problem);
                    case PackageExceptionHandler.TYPE_PACKAGE_NOT_FOUND:
                        throw (PackageNotFoundException) PackageExceptionHandler.createException(problem);
                    default:
                        throw new RuntimeException("The problem type " + problem.getError() + " was not known");
                }
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }
    }

    @Override
    public void updateVersionSet(String accountId, String versionSetName, UpdateVersionSetRequest request) throws VersionSetDoseNotExistsException,
            PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName), "Version-set name is required");
        Preconditions.checkNotNull(request);
        request.validate();

        UpdateVersionSetRestRequest restRequest = UpdateVersionSetRestRequest.from(request);

        HttpResponse<String> httpResponse;
        try {
            String test = objectMapper.writeValueAsString(restRequest);
            log.info("Serilized: " + test);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/account/" + accountId + "/version-set/" + versionSetName)
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call version set service to update version-set '{}' for account '{}'", versionSetName,
                    accountId);
            throw new RuntimeException(e);
        }
        ProblemDetailRestResponse problem;
        switch (httpResponse.statusCode()) {
            case 200:
                break;
            case 401:
            case 403:
                log.error("Got unauthorized response from Version set service");
                throw new UnauthorizedException();
            case 404:
                log.warn("Got Not Found (404) response from Version set service with body: " + httpResponse.body());
                problem = ProblemDetailRestResponse.from(httpResponse.body());
                switch (problem.getError()) {
                    case VersionSetExceptionHandler.TYPE_VERSION_SET_NOT_FOUND:
                        throw (VersionSetDoseNotExistsException) VersionSetExceptionHandler.createException(problem);
                    case PackageExceptionHandler.TYPE_PACKAGE_NOT_FOUND:
                        throw (PackageNotFoundException) PackageExceptionHandler.createException(problem);
                    default:
                        throw new RuntimeException("The problem type " + problem.getError() + " was not known");
                }
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }
    }

    @Override
    public String createVersionRevision(String accountId, String versionSetName, List<ArchipelagoBuiltPackage> packages, ArchipelagoPackage target)
            throws VersionSetDoseNotExistsException, MissingTargetPackageException, PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName), "Version set name is required");
        Preconditions.checkArgument(packages != null && packages.size() > 0, "Packages are required");

        CreateVersionSetRevisionRestRequest restRequest = CreateVersionSetRevisionRestRequest.builder()
                .packages(packages.stream().map(ArchipelagoBuiltPackage::toString).collect(Collectors.toList()))
                .target(target == null ? null : target.getNameVersion())
                .build();

        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/account/" + accountId + "/version-set/" + versionSetName)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call version set service to create a version set revision for account '{}'", accountId);
            throw new RuntimeException(e);
        }
        ProblemDetailRestResponse problem;
        switch (httpResponse.statusCode()) {
            case 200: // Ok
                return this.parseOrThrow(httpResponse.body(), CreateVersionSetRevisionRestResponse.class).getRevisionId();
            case 400: // PRECONDITION FAILED
                log.warn("Got 400 response from Version set service with body: " + httpResponse.body());
                problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (MissingTargetPackageException) VersionSetExceptionHandler.createException(problem);
            case 401:
            case 403:
                log.error("Got unauthorized response from Version set service");
                throw new UnauthorizedException();
            case 404:
                log.warn("Got Not Found (404) response from Version set service with body: " + httpResponse.body());
                problem = ProblemDetailRestResponse.from(httpResponse.body());
                switch (problem.getError()) {
                    case VersionSetExceptionHandler.TYPE_VERSION_SET_NOT_FOUND:
                        throw (VersionSetDoseNotExistsException) VersionSetExceptionHandler.createException(problem);
                    case PackageExceptionHandler.TYPE_PACKAGE_NOT_FOUND:
                        throw (PackageNotFoundException) PackageExceptionHandler.createException(problem);
                    default:
                        throw new RuntimeException("The problem type " + problem.getError() + " was not known");
                }
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }
    }

    @Override
    public VersionSet getVersionSet(String accountId, String versionSetName) throws VersionSetDoseNotExistsException, UnauthorizedException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName), "Version set name is required");

        VersionSetRestResponse response;
        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/account/" + accountId + "/version-set/" + versionSetName)
                    .GET()
                    .build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call version set service to get a version set from account '{}'", accountId);
            throw new RuntimeException(e);
        }
        switch (httpResponse.statusCode()) {
            case 200: // Ok
                response = this.parseOrThrow(httpResponse.body(), VersionSetRestResponse.class);
                break;
            case 401:
            case 403:
                log.error("Got unauthorized response from Version set service");
                throw new UnauthorizedException();
            case 404: // Not found
                log.warn("Got Not Found (404) response from Version set service with body: " + httpResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (VersionSetDoseNotExistsException) VersionSetExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }

        return response.toInternal();
    }

    @Override
    public VersionSetRevision getVersionSetPackages(String accountId, String versionSetName, String revisionId) throws VersionSetDoseNotExistsException, UnauthorizedException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName), "Version set name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(revisionId), "Version set revision is required");

        VersionSetRevisionRestResponse response;
        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/account/" + accountId + "/version-set/" + versionSetName + "/" + revisionId)
                    .GET()
                    .build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call version set service to get the packages in version set {} from account '{}'", versionSetName, accountId);
            throw new RuntimeException(e);
        }

        switch (httpResponse.statusCode()) {
            case 200: // Ok
                response = this.parseOrThrow(httpResponse.body(), VersionSetRevisionRestResponse.class);
                break;
            case 401:
            case 403:
                log.error("Got unauthorized response from Version set service");
                throw new UnauthorizedException();
            case 404:
                log.warn("Got Not Found (404) response from Version set service with body: " + httpResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (VersionSetDoseNotExistsException) VersionSetExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }

        return response.toInternal();
    }

    @Override
    public List<VersionSetCallback> getCallbacks(String accountId, String versionSetName) throws VersionSetDoseNotExistsException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName), "Version set name is required");

        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/account/" + accountId + "/version-set/" + versionSetName + "/callbacks")
                    .GET()
                    .build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call version set service to get the packages in version set {} from account '{}'", versionSetName, accountId);
            throw new RuntimeException(e);
        }

        switch (httpResponse.statusCode()) {
            case 200: // Ok
                return this.parseOrThrow(httpResponse.body(), VersionSetCallbacksRestResponse.class).getCallbacks()
                        .stream().map(VersionSetCallbackRestResponse::toInternal).collect(Collectors.toList());
            case 401:
            case 403:
                log.error("Got unauthorized response from Version set service");
                throw new UnauthorizedException();
            case 404:
                log.warn("Got Not Found (404) response from Version set service with body: " + httpResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (VersionSetDoseNotExistsException) VersionSetExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }
    }

    @Override
    public void deleteCallback(String accountId, String versionSetName, String id) throws VersionSetDoseNotExistsException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName), "Version set name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(id), "Callback id is required");

        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/account/" + accountId + "/version-set/" + versionSetName + "/callbacks/" + id)
                    .DELETE()
                    .build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call version set service to add a callback for account '{}'", accountId);
            throw new RuntimeException(e);
        }
        ProblemDetailRestResponse problem;
        switch (httpResponse.statusCode()) {
            case 200: // Ok
                return;
            case 401:
            case 403:
                log.error("Got unauthorized response from Version set service");
                throw new UnauthorizedException();
            case 404:
                log.warn("Got Not Found (404) response from Version set service with body: " + httpResponse.body());
                problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (VersionSetDoseNotExistsException) VersionSetExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }
    }

    @Override
    public void addCallback(String accountId, String versionSetName, String url) throws VersionSetDoseNotExistsException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName), "Version set name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(url), "Callback url is");

        AddCallbackRestRequest request = AddCallbackRestRequest.builder()
                .url(url)
                .build();
        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/account/" + accountId + "/version-set/" + versionSetName + "/callbacks")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call version set service to add a callback for account '{}'", accountId);
            throw new RuntimeException(e);
        }
        ProblemDetailRestResponse problem;
        switch (httpResponse.statusCode()) {
            case 200: // Ok
                return;
            case 401:
            case 403:
                log.error("Got unauthorized response from Version set service");
                throw new UnauthorizedException();
            case 404:
                log.warn("Got Not Found (404) response from Version set service with body: " + httpResponse.body());
                problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (VersionSetDoseNotExistsException) VersionSetExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }
    }

    private RuntimeException logAndReturnExceptionForUnknownStatusCode(HttpResponse<String> restResponse) {
        log.error("Unknown response from Version set service status code " + restResponse.statusCode() +
                " body: " + restResponse.body());
        return new RuntimeException("Unknown response from Version set service status code " + restResponse.statusCode() +
                " body: " + restResponse.body());
    }
}
