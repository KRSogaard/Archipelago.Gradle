package build.archipelago.harbor.client;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.clients.rest.OAuthRestClient;
import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.rest.models.errors.ProblemDetailRestResponse;
import build.archipelago.common.versionset.*;
import build.archipelago.harbor.client.models.CreatePackageRequest;
import build.archipelago.packageservice.client.PackageExceptionHandler;
import build.archipelago.packageservice.exceptions.*;
import build.archipelago.packageservice.models.*;
import build.archipelago.packageservice.models.rest.*;
import build.archipelago.versionsetservice.client.VersionSetExceptionHandler;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.versionsetservice.models.rest.*;
import com.google.common.base.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;

@Slf4j
public class RestHarborClient extends OAuthRestClient implements HarborClient {
    private static final String OAUTH2_SCOPES = "http://harbor.archipelago.build/versionsets http://harbor.archipelago.build/packages http://harbor.archipelago.build/builds";
    private static final String OAUTH2_TOKENURL = "https://archipelago.auth.us-west-2.amazoncognito.com/oauth2/token";

    public RestHarborClient(String baseUrl, String tokenUrl, String oauthToken, String audience) {
        super(baseUrl, tokenUrl, oauthToken, audience);
    }

    @Override
    public VersionSet getVersionSet(String versionSetName) throws VersionSetDoseNotExistsException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName), "Version set name is required");

        VersionSetRestResponse response;
        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/version-set/" + versionSetName)
                    .GET()
                    .build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call harbor service to get a version set '{}'", versionSetName);
            throw new RuntimeException(e);
        }
        switch (httpResponse.statusCode()) {
            case 200: // Ok
                response = this.parseOrThrow(httpResponse.body(), VersionSetRestResponse.class);
                break;
            case 401:
            case 403:
                log.error("Got unauthorized response from Harbor service");
                throw new UnauthorizedException();
            case 404:
                log.warn("Got Not Found (404) response from Harbor service with body: " + httpResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (VersionSetDoseNotExistsException) VersionSetExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }

        return response.toInternal();
    }

    @Override
    public VersionSetRevision getVersionSetRevision(String versionSetName, String revisionId) throws VersionSetDoseNotExistsException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName), "Version set name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(revisionId), "Version set revision is required");

        VersionSetRevisionRestResponse response;
        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/version-set/" + versionSetName + "/" + revisionId)
                    .GET()
                    .build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call harbor service to get a version set '{}' revision '{}'", versionSetName, revisionId);
            throw new RuntimeException(e);
        }

        switch (httpResponse.statusCode()) {
            case 200: // Ok
                response = this.parseOrThrow(httpResponse.body(), VersionSetRevisionRestResponse.class);
                break;
            case 401:
            case 403:
                log.error("Got unauthorized response from Harbor service");
                throw new UnauthorizedException();
            case 404:
                log.warn("Got Not Found (404) response from Harbor service with body: " + httpResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (VersionSetDoseNotExistsException) VersionSetExceptionHandler.createException(problem);
            default:
                throw new RuntimeException("Was unable to create the version set revision with status code " + httpResponse.statusCode());
        }

        return response.toInternal();
    }

    @Override
    public Path getBuildArtifact(ArchipelagoBuiltPackage pkg, Path directory) throws PackageNotFoundException {
        Preconditions.checkNotNull(pkg, "Name and Version is required");
        Preconditions.checkNotNull(directory, "A save location is required");

        Path filePath = Paths.get(
                directory.toString(),
                String.format("%s.zip", java.util.UUID.randomUUID().toString()));

        if (!Files.isDirectory(directory)) {
            log.info("Creating directory '{}'", directory.toString());
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                log.error("Got unknown error when trying to create the directory '" + directory.toString() + "'", e);
                throw new RuntimeException(e);
            }
        }

        GetBuildArtifactResponse response = this.getBuildArtifact(pkg);

        log.debug("Got a signed url from Harbor service to download the artifact for '{}', url '{}'",
                pkg.getBuiltPackageName(), response.getUrl());
        HttpResponse<Path> restPathResponse;
        try {
            HttpRequest request = HttpRequest.newBuilder(new URI(response.getUrl()))
                    .GET()
                    .build();
            restPathResponse = client.send(request, HttpResponse.BodyHandlers.ofFile(filePath));
        } catch (Exception e) {
            log.error("Got an unknown error when we tried to download a package artifact for '{}' from S3", pkg.getBuiltPackageName());
            throw new RuntimeException(e);
        }
        switch (restPathResponse.statusCode()) {
            case 200: // Ok
                return restPathResponse.body();
            case 401:
            case 403:
                log.error("Got unauthorized response from S3");
                throw new UnauthorizedException();
            case 404:
                throw new PackageNotFoundException(pkg);
            default:
                log.error("Unknown response from S3 when we tried to fetch the file, status code " + restPathResponse.statusCode());
                throw new RuntimeException("Unknown response from S3 when we tried to fetch the file, status code " + restPathResponse.statusCode());
        }
    }

    @Override
    public GetBuildArtifactResponse getBuildArtifact(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException {
        GetBuildArtifactRestResponse restResponse;
        HttpResponse<String> httpResponse;
        try {
            HttpRequest request = this.getOAuthRequest("/package/" + pkg.getName() + "/" + pkg.getVersion() + "/" + pkg.getHash() + "/artifact")
                    .GET()
                    .build();
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call Harbor service to get download url for artifact '{}'",
                    pkg.getBuiltPackageName());
            throw new RuntimeException(e);
        }
        switch (httpResponse.statusCode()) {
            case 200: // Ok
                restResponse = this.parseOrThrow(httpResponse.body(), GetBuildArtifactRestResponse.class);
                return restResponse.toInternal();
            case 401:
            case 403:
                log.error("Got unauthorized response from Harbor service");
                throw new UnauthorizedException();
            case 404:
                log.warn("Got Not Found (404) response from Harbor service with body: " + httpResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (PackageNotFoundException) PackageExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }
    }

    @Override
    public void createPackage(CreatePackageRequest request) throws PackageExistsException {
        Preconditions.checkNotNull(request);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getName()));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getDescription()));

        CreatePackageRestRequest restRequest = CreatePackageRestRequest.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/package")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .header("accept", "application/json")
                    .build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        switch (httpResponse.statusCode()) {
            case 200: // Ok
                return;
            case 401:
            case 403:
                log.error("Got unauthorized response from Harbor service");
                throw new UnauthorizedException();
            case 409:
                log.warn("Got Conflict (409) response from Harbor service with body: " + httpResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (PackageExistsException) PackageExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }
    }

    @Override
    public String getConfig(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException {
        Preconditions.checkNotNull(pkg);

        HttpResponse<String> httpResponse;
        try {
            HttpRequest request = this.getOAuthRequest("/package/" + pkg.getName() + "/" + pkg.getVersion() + "/" + pkg.getHash() + "/config")
                    .GET()
                    .build();
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        switch (httpResponse.statusCode()) {
            case 200:
                return httpResponse.body();
            case 401:
            case 403:
                log.error("Got unauthorized response from Harbor service");
                throw new UnauthorizedException();
            case 404:
                log.warn("Got Not Found (404) response from Harbor service with body: " + httpResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (PackageNotFoundException) PackageExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }
    }

    @Override
    public PackageDetails getPackage(String name) throws PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));

        GetPackageRestResponse response;
        HttpResponse<String> httpResponse;
        try {
            HttpRequest request = this.getOAuthRequest("/package/" + name)
                    .GET()
                    .build();

            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        switch (httpResponse.statusCode()) {
            case 200: // Ok
                response = this.parseOrThrow(httpResponse.body(), GetPackageRestResponse.class);
                break;
            case 401:
            case 403:
                log.error("Got unauthorized response from Harbor service");
                throw new UnauthorizedException();
            case 404:
                log.warn("Got Not Found (404) response from Harbor service with body: " + httpResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (PackageNotFoundException) PackageExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }

        return response.toInternal();
    }

    private RuntimeException logAndReturnExceptionForUnknownStatusCode(HttpResponse<String> restResponse) {
        log.error("Unknown response from Harbor service status code " + restResponse.statusCode() +
                " body: " + restResponse.body());
        return new RuntimeException("Unknown response from Harbor service status code " + restResponse.statusCode() +
                " body: " + restResponse.body());
    }
}
