package build.archipelago.harbor.client;

import build.archipelago.common.*;
import build.archipelago.common.clients.rest.OAuthRestClient;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.versionset.*;
import build.archipelago.packageservice.client.models.*;
import build.archipelago.packageservice.client.rest.models.*;
import build.archipelago.versionsetservice.client.rest.models.*;
import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.http.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
public class RestHarborClient extends OAuthRestClient implements HarborClient {
    public RestHarborClient(String baseUrl, String tokenUrl, String oauthToken, String audience) {
        super(baseUrl, tokenUrl, oauthToken, audience);
    }

    @Override
    public VersionSet getVersionSet(String versionSetName) throws VersionSetDoseNotExistsException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName), "Version set name is required");

        RestVersionSetResponse response;
        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = getOAuthRequest("/version-set/" + versionSetName)
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
    public VersionSetRevision getVersionSetRevision(String versionSetName, String revisionId) throws VersionSetDoseNotExistsException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName), "Version set name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(revisionId), "Version set revision is required");

        RestVersionSetRevisionResponse response;
        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = getOAuthRequest("/version-set/" + versionSetName + "/" + revisionId)
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

    @Override
    public Path getBuildArtifact(ArchipelagoBuiltPackage pkg, Path directory) throws PackageNotFoundException {
        Preconditions.checkNotNull(pkg, "Name and Version is required");
        Preconditions.checkNotNull(directory, "A save location is required");

        Path filePath = Paths.get(
                directory.toString(),
                String.format("%s.zip", java.util.UUID.randomUUID().toString()));

        if (!Files.isDirectory(directory)) {
            log.info("Creating directory \"%s\"", directory.toString());
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        HttpResponse<Path> restResponse;
        try {
            HttpRequest request = getOAuthRequest("/package/" + pkg.getName() + "/" + pkg.getVersion() + "/" + pkg.getHash() + "/artifact")
                    .GET()
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofFile(filePath));
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        switch (restResponse.statusCode()) {
            case 401:
                throw new UnauthorizedException();
            case 404: // Not found
                throw new PackageNotFoundException(pkg);
            case 200: // Ok
                return restResponse.body();
            default:
                throw new RuntimeException("Unknown response " + restResponse.statusCode());
        }
    }

    @Override
    public void createPackage(CreatePackageRequest request) throws PackageExistsException {
        Preconditions.checkNotNull(request);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getName()));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getDescription()));

        RestCreatePackageRequest restRequest = new RestCreatePackageRequest(
                request.getName(),
                request.getDescription()
        );

        HttpResponse<String> response;
        try {
            HttpRequest httpRequest = getOAuthRequest("/package")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .header("accept", "application/json")
                    .build();
            response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        switch (response.statusCode()) {
            case 401:
                throw new UnauthorizedException();
            case 409: // Conflict
                throw new PackageExistsException(request.getName());
            case 200: // Ok
                return;
            default:
                throw new RuntimeException("Unknown response " + response.statusCode());
        }
    }

    @Override
    public String getConfig(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException {
        Preconditions.checkNotNull(pkg);

        RestGetPackageBuildResponse response;
        HttpResponse<String> restResponse;
        try {
            HttpRequest request = getOAuthRequest( "/package/" + pkg.getName() + "/" + pkg.getVersion() + "/" + pkg.getHash() + "/config")
                    .GET()
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        switch (restResponse.statusCode()) {
            case 200:
                try {
                    return objectMapper.readValue(restResponse.body(), RestGetPackageBuildResponse.class)
                            .getConfig();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            case 401:
                throw new UnauthorizedException();
            case 404: // Conflict
                throw new PackageNotFoundException(pkg);
            default:
                throw new RuntimeException("Unknown response " + restResponse.statusCode());
        }
    }

    @Override
    public GetPackageResponse getPackage(String name) throws PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));

        RestGetPackageResponse response;
        HttpResponse<String> restResponse;
        try {
            HttpRequest request = getOAuthRequest("/package/" + name)
                    .GET()
                    .build();

            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        switch (restResponse.statusCode()) {
            case 401:
                throw new UnauthorizedException();
            case 404: // Not found
                throw new PackageNotFoundException(name);
            case 200: // Ok
                try {
                    response = objectMapper.readValue(restResponse.body(), RestGetPackageResponse.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                throw new RuntimeException("Unknown response " + restResponse.statusCode());
        }

        ImmutableList.Builder<GetPackageResponse.Version> versions = ImmutableList.builder();
        response.getVersions().forEach(x -> versions.add(new GetPackageResponse.Version(
                x.getVersion(), x.getLatestBuildHash(), Instant.ofEpochMilli(x.getLatestBuildTime())
        )));

        return GetPackageResponse.builder()
                .name(response.getName())
                .description(response.getDescription())
                .created(Instant.ofEpochMilli(response.getCreated()))
                .versions(versions.build())
                .build();
    }
}
