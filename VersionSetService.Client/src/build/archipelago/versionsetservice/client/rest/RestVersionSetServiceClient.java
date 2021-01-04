package build.archipelago.versionsetservice.client.rest;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.clients.rest.OAuthRestClient;
import build.archipelago.common.exceptions.MissingTargetPackageException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.exceptions.VersionSetExistsException;
import build.archipelago.common.versionset.Revision;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import build.archipelago.versionsetservice.client.models.CreateVersionSetRequest;
import build.archipelago.versionsetservice.client.rest.models.RestCreateVersionSetRequest;
import build.archipelago.versionsetservice.client.rest.models.RestCreateVersionSetRevisionRequest;
import build.archipelago.versionsetservice.client.rest.models.RestCreateVersionSetRevisionResponse;
import build.archipelago.versionsetservice.client.rest.models.RestVersionSetResponse;
import build.archipelago.versionsetservice.client.rest.models.RestVersionSetRevisionResponse;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
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
    public void createVersionSet(String accountId, CreateVersionSetRequest request)
            throws VersionSetExistsException, VersionSetDoseNotExistsException, PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkNotNull(request);
        request.validate();

        RestCreateVersionSetRequest restRequest = new RestCreateVersionSetRequest(
                request.getName(),
                request.getTargets().stream().map(ArchipelagoPackage::getNameVersion).collect(Collectors.toList()),
                request.getParent() != null && request.getParent().isPresent() ? request.getParent().get() : null
        );

        HttpResponse<Void> httpResponse;
        try {
            HttpRequest httpRequest = getOAuthRequest("/account/" + accountId + "/version-sets")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.discarding());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        switch (httpResponse.statusCode()) {
            case 200: // Ok
                break;
            case 401:
                throw new UnauthorizedException();
            case 409: // Conflict
                throw new VersionSetExistsException(request.getName());
            case 404: // Not found
                if (request.getParent() == null || request.getParent().isEmpty()) {
                    log.error("Create version set returned version set not found status code, " +
                            "but no parent this should not happen.");
                    throw new RuntimeException("Create version set returned version set not found status code, " +
                            "but no parent this should not happen.");
                }
                throw new VersionSetDoseNotExistsException(request.getParent().get());
            case 406: // Not acceptable
                if (request.getParent() != null && request.getParent().isPresent()) {
                    throw new PackageNotFoundException(request.getParent().get());
                }
                throw new RuntimeException("Got not acceptable, but not parent was requested");
            default:
                throw new RuntimeException("Was unable to create the version set with status code " + httpResponse.statusCode());
        }
    }

    @Override
    public String createVersionRevision(String accountId, String versionSetName, List<ArchipelagoBuiltPackage> packages)
            throws VersionSetDoseNotExistsException, MissingTargetPackageException, PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName), "Version set name is required");
        Preconditions.checkArgument(packages != null && packages.size() > 0, "Packages are required");

        RestCreateVersionSetRevisionRequest restRequest = new RestCreateVersionSetRevisionRequest(
                packages.stream().map(ArchipelagoBuiltPackage::toString).collect(Collectors.toList())
        );

        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = getOAuthRequest("/account/" + accountId + "/version-set/" + versionSetName)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        switch (httpResponse.statusCode()) {
            case 200: // Ok
                try {
                    RestCreateVersionSetRevisionResponse response = objectMapper.readValue(httpResponse.body(), RestCreateVersionSetRevisionResponse.class);
                    return response.getRevisionId();
                } catch (IOException e) {
                    log.error(String.format("Was unable to parse the string \"%s\" as a RestCreateVersionSetRevisionResponse object", httpResponse.body()), e);
                    throw new RuntimeException("Was unable to parse the object as RestCreateVersionSetRevisionResponse", e);
                }
            case 401:
                throw new UnauthorizedException();
            case 404: // Not found
                throw new VersionSetDoseNotExistsException(versionSetName);
            case 406: // Not acceptable
                throw new PackageNotFoundException(httpResponse.body());
            case 412: // PRECONDITION FAILED
                throw new MissingTargetPackageException();
            default:
                throw new RuntimeException("Was unable to create the version set revision with status code " + httpResponse.statusCode());
        }
    }

    @Override
    public VersionSet getVersionSet(String accountId, String versionSetName) throws VersionSetDoseNotExistsException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName), "Version set name is required");

        RestVersionSetResponse response;
        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = getOAuthRequest("/account/" + accountId + "/version-set/" + versionSetName)
                    .GET()
                    .build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        switch (httpResponse.statusCode()) {
            case 200: // Ok
                try {
                    response = objectMapper.readValue(httpResponse.body(), RestVersionSetResponse.class);
                } catch (IOException e) {
                    log.error(String.format("Failed to parse the string \"%s\" as a RestVersionSetResponse object", httpResponse.body()), e);
                    throw new RuntimeException("Failed to parse RestVersionSetResponse", e);
                }
                break;
            case 401:
                throw new UnauthorizedException();
            case 404: // Not found
                throw new VersionSetDoseNotExistsException(versionSetName);
            default:
                throw new RuntimeException("Was unable to get the version set revision with status code " + httpResponse.statusCode());
        }

        return VersionSet.builder()
                .name(response.getName())
                .created(Instant.ofEpochMilli(response.getCreated()))
                .parent(response.getParent())
                .targets(response.getTargets().stream().map(ArchipelagoPackage::parse).collect(Collectors.toList()))
                .revisions(response.getRevisions().stream().map(x ->
                        Revision.builder()
                        .revisionId(x.getRevisionId())
                        .created(Instant.ofEpochMilli(x.getCreated()))
                        .build()).collect(Collectors.toList()))
                .latestRevision(response.getLatestRevision())
                .latestRevisionCreated(response.getLatestRevisionCreated() != null ?
                        Instant.ofEpochMilli(response.getLatestRevisionCreated()) :
                        null)
                .build();
    }

    @Override
    public VersionSetRevision getVersionSetPackages(String accountId, String versionSetName, String revisionId) throws VersionSetDoseNotExistsException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName), "Version set name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(revisionId), "Version set revision is required");

        RestVersionSetRevisionResponse response;
        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = getOAuthRequest("/account/" + accountId + "/version-set/" + versionSetName + "/" + revisionId)
                    .GET()
                    .build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        switch (httpResponse.statusCode()) {
            case 200: // Ok
                try {
                    response = objectMapper.readValue(httpResponse.body(), RestVersionSetRevisionResponse.class);
                } catch (IOException e) {
                    log.error(String.format("Failed to parse the string \"%s\" as a RestVersionSetRevisionResponse object", httpResponse.body()), e);
                    throw new RuntimeException("Failed to parse RestVersionSetRevisionResponse", e);
                }
                break;
            case 401:
                throw new UnauthorizedException();
            case 404: // Not found
                throw new VersionSetDoseNotExistsException(versionSetName);
            default:
                throw new RuntimeException("Was unable to create the version set revision with status code " + httpResponse.statusCode());
        }

        return VersionSetRevision.builder()
                .created(Instant.ofEpochMilli(response.getCreated()))
                .packages(response.getPackages().stream().map(ArchipelagoBuiltPackage::parse).collect(Collectors.toList()))
                .build();
    }
}
