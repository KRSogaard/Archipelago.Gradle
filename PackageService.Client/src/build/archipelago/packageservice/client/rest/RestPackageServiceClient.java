package build.archipelago.packageservice.client.rest;

import build.archipelago.common.*;
import build.archipelago.common.clients.rest.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.packageservice.client.*;
import build.archipelago.packageservice.client.models.*;
import build.archipelago.packageservice.client.rest.models.*;
import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class RestPackageServiceClient extends OAuthRestClient implements PackageServiceClient {

    private static final String OAUTH2_AUDIENCE = "http://packageservice.archipelago.build";
    private static final String OAUTH2_TOKENURL = "https://dev-1nl95fdx.us.auth0.com/oauth/token";

    public RestPackageServiceClient(String endpoint, String clientId, String clientSecret) {
        super(endpoint, OAUTH2_TOKENURL, clientId, clientSecret, OAUTH2_AUDIENCE);
    }

    @Override
    public void createPackage(String accountId, CreatePackageRequest request) throws PackageExistsException, UnauthorizedException {
        Preconditions.checkNotNull(request);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getName()));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getDescription()));

        RestCreatePackageRequest restRequest = new RestCreatePackageRequest(
                request.getName(),
                request.getDescription()
        );

        HttpResponse<String> response;
        try {
            HttpRequest httpRequest = getOAuthRequest("/account/" + accountId + "/package")
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
            case 409: // Conflict
                throw new PackageExistsException(request.getName());
            case 200: // Ok
                return;
            default:
                throw new RuntimeException("Unknown response " + response.statusCode());
        }
    }

    @Override
    public GetPackageResponse getPackage(String accountId, String name) throws PackageNotFoundException, UnauthorizedException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));

        RestGetPackageResponse response;
        HttpResponse<String> restResponse;
        try {
            HttpRequest request = getOAuthRequest("/account/" + accountId + "/package/" + name)
                    .GET()
                    .build();

            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        response = validateResponse(restResponse, name, RestGetPackageResponse.class);

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

    @Override
    public PackageBuildsResponse getPackageBuilds(String accountId, ArchipelagoPackage pkg) throws PackageNotFoundException, UnauthorizedException {
        Preconditions.checkNotNull(pkg);

        RestPackageBuildsResponse response;
        HttpResponse<String> restResponse;
        try {
            HttpRequest request = getOAuthRequest("/account/" + accountId + "/package/" + pkg.getName() + "/" + pkg.getVersion())
                    .GET()
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        response = validateResponse(restResponse, pkg.toString(), RestPackageBuildsResponse.class);

        ImmutableList.Builder<PackageBuildsResponse.Build> builds = ImmutableList.builder();
        response.getBuilds().forEach(x -> builds.add(new PackageBuildsResponse.Build(
                x.getHash(), Instant.ofEpochMilli(x.getCreated())
        )));

        return PackageBuildsResponse.builder()
                .builds(builds.build())
                .build();
    }

    @Override
    public GetPackageBuildResponse getPackageBuild(String accountId, ArchipelagoBuiltPackage pkg) throws PackageNotFoundException, UnauthorizedException {
        Preconditions.checkNotNull(pkg);

        RestGetPackageBuildResponse response;
        HttpResponse<String> restResponse;
        try {
            HttpRequest request = getOAuthRequest("/account/" + accountId + "/package/" + pkg.getName() + "/" + pkg.getVersion() + "/" + pkg.getHash())
                    .GET()
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        response = validateResponse(restResponse, pkg.toString(), RestGetPackageBuildResponse.class);

        return GetPackageBuildResponse.builder()
                .hash(response.getHash())
                .created(Instant.ofEpochMilli(response.getCreated()))
                .config(response.getConfig())
                .gitCommit(response.getGitCommit())
                .gitBranch(response.getGitBranch())
                .build();
    }

    @Override
    public ArchipelagoBuiltPackage getPackageByGit(String accountId, String packageName, String branch, String commit) throws PackageNotFoundException, UnauthorizedException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(packageName));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(branch));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(commit));

        ArchipelagoBuiltPackageResponse response;
        HttpResponse<String> restResponse;
        try {
            HttpRequest request = getOAuthRequest("/account/" + accountId + "/package/" + packageName + "/git/" + branch + "/" + commit)
                    .GET()
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        response = validateResponse(restResponse, packageName + " (B: " + branch +", C:" + commit + ")",
                ArchipelagoBuiltPackageResponse.class);

        return new ArchipelagoBuiltPackage(response.getName(), response.getVersion(), response.getHash());
    }

    @Override
    public PackageVerificationResult<ArchipelagoPackage> verifyPackagesExists(String accountId, List<ArchipelagoPackage> packages) throws UnauthorizedException {
        Preconditions.checkNotNull(packages);
        Preconditions.checkArgument(packages.size() > 0);

        RestVerificationResponse response;
        RestVerificationRequest restRequest = new RestVerificationRequest(
                packages.stream().map(ArchipelagoPackage::getNameVersion).collect(Collectors.toList()));
        HttpResponse<String> restResponse;
        try {
            HttpRequest request = getOAuthRequest("/account/" + accountId + "/package/verify-packages")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        switch (restResponse.statusCode()) {
            case 200: // Ok
                try {
                    response = objectMapper.readValue(restResponse.body(), RestVerificationResponse.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                throw new RuntimeException("Unknown response " + restResponse.statusCode());
        }

        return PackageVerificationResult.<ArchipelagoPackage>builder()
                .missingPackages(ImmutableList.copyOf(
                        response.getMissing().stream().map(ArchipelagoPackage::parse).collect(Collectors.toList())))
                .build();
    }

    @Override
    public PackageVerificationResult<ArchipelagoBuiltPackage> verifyBuildsExists(String accountId, List<ArchipelagoBuiltPackage> packages) throws UnauthorizedException {
        Preconditions.checkNotNull(packages);
        Preconditions.checkArgument(packages.size() > 0);

        RestVerificationResponse response;
        RestVerificationRequest restRequest = new RestVerificationRequest(
                packages.stream().map(ArchipelagoBuiltPackage::toString).collect(Collectors.toList()));
        HttpResponse<String> restResponse;
        try {
            HttpRequest request = getOAuthRequest("/account/" + accountId + "/package/verify-builds")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        switch (restResponse.statusCode()) {
            case 200: // Ok
                try {
                    response = objectMapper.readValue(restResponse.body(), RestVerificationResponse.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                throw new RuntimeException("Unknown response " + restResponse.statusCode());
        }

        return PackageVerificationResult.<ArchipelagoBuiltPackage>builder()
                .missingPackages(ImmutableList.copyOf(
                        response.getMissing().stream().map(ArchipelagoBuiltPackage::parse).collect(Collectors.toList())))
                .build();
    }

    @Override
    public String uploadBuiltArtifact(String accountId, UploadPackageRequest request, Path file) throws PackageNotFoundException, UnauthorizedException {
        Preconditions.checkNotNull(request);
        Preconditions.checkNotNull(request.getPkg());
        Preconditions.checkNotNull(file);
        Preconditions.checkArgument(Files.exists(file), "File did not exists");

        MultiPartBodyPublisher publisher = new MultiPartBodyPublisher()
                .addPart("buildArtifact", file)
                .addPart("config", request.getConfig())
                .addPart("gitCommit", request.getGitCommit())
                .addPart("gitBranch", request.getGitBranch());

        RestArtifactUploadResponse response;
        HttpResponse<String> restResponse;
        try {
            String url = baseUrl + "/account/" + accountId + "/artifact/" + request.getPkg().getName() + "/" + request.getPkg().getVersion();
            HttpRequest restTequest = addOauth(HttpRequest.newBuilder(new URI(url)))
                    .header("content-type", "multipart/form-data; boundary=" + publisher.getBoundary())
                    .header("accept", "application/json")
                    .POST(publisher.build())
                    .build();
            restResponse = client.send(restTequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        response = validateResponse(restResponse, request.getPkg().toString(), RestArtifactUploadResponse.class);
        return response.getHash();
    }

    @Override
    public Path getBuildArtifact(String accountId, ArchipelagoBuiltPackage pkg, Path directory) throws PackageNotFoundException, IOException, UnauthorizedException {
        Preconditions.checkNotNull(pkg, "Name and Version is required");
        Preconditions.checkNotNull(directory, "A save location is required");

        Path filePath = Paths.get(
                directory.toString(),
                String.format("%s.zip", java.util.UUID.randomUUID().toString()));

        if (!Files.isDirectory(directory)) {
            log.info("Creating directory \"%s\"", directory.toString());
            Files.createDirectories(directory);
        }
        HttpResponse<Path> restResponse;
        try {
            HttpRequest request = getOAuthRequest("/account/" + accountId + "/artifact/" + pkg.getName() + "/" + pkg.getVersion() + "/" + pkg.getHash())
                    .GET()
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofFile(filePath));
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        switch (restResponse.statusCode()) {
            case 404: // Not found
                throw new PackageNotFoundException(pkg);
            case 200: // Ok
                return restResponse.body();
            default:
                throw new RuntimeException("Unknown response " + restResponse.statusCode());
        }
    }

    private <T> T validateResponse(HttpResponse<String> response, String packageName, Function<String, T> onOk) throws PackageNotFoundException {

        switch (response.statusCode()) {
            case 404: // Not found
                throw new PackageNotFoundException(packageName);
            case 200: // Ok
                return onOk.apply(response.body());
            default:
                throw new RuntimeException("Unknown response " + response.statusCode());
        }
    }
    private <T> T validateResponse(HttpResponse<String> response, String packageName, Class<T> outputCalls) throws PackageNotFoundException {
        return validateResponse(response, packageName, s -> {
            try {
                return objectMapper.readValue(response.body(), outputCalls);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
