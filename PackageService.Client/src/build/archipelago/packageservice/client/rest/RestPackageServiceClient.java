package build.archipelago.packageservice.client.rest;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.clients.rest.MultiPartBodyPublisher;
import build.archipelago.common.clients.rest.OAuthRestClient;
import build.archipelago.common.exceptions.PackageExistsException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.CreatePackageRequest;
import build.archipelago.packageservice.client.models.GetPackageBuildResponse;
import build.archipelago.packageservice.client.models.GetPackageResponse;
import build.archipelago.packageservice.client.models.GetPackagesResponse;
import build.archipelago.packageservice.client.models.PackageBuildsResponse;
import build.archipelago.packageservice.client.models.PackageVerificationResult;
import build.archipelago.packageservice.client.models.UploadPackageRequest;
import build.archipelago.packageservice.client.rest.models.ArchipelagoBuiltPackageResponse;
import build.archipelago.packageservice.client.rest.models.GetBuildArtifactRestResponse;
import build.archipelago.packageservice.client.rest.models.RestArtifactUploadResponse;
import build.archipelago.packageservice.client.rest.models.RestCreatePackageRequest;
import build.archipelago.packageservice.client.rest.models.RestGetPackageBuildResponse;
import build.archipelago.packageservice.client.rest.models.RestGetPackageResponse;
import build.archipelago.packageservice.client.rest.models.RestGetPackagesResponse;
import build.archipelago.packageservice.client.rest.models.RestPackageBuildsResponse;
import build.archipelago.packageservice.client.rest.models.RestVerificationRequest;
import build.archipelago.packageservice.client.rest.models.RestVerificationResponse;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RestPackageServiceClient extends OAuthRestClient implements PackageServiceClient {

    private static final String OAUTH2_SCOPES = "http://packageservice.archipelago.build/read http://packageservice.archipelago.build/write";
    private static final String OAUTH2_TOKENURL = "https://archipelago.auth.us-west-2.amazoncognito.com/oauth2/token";

    public RestPackageServiceClient(String endpoint, String clientId, String clientSecret) {
        super(endpoint, OAUTH2_TOKENURL, clientId, clientSecret, OAUTH2_SCOPES);
    }

    @Override
    public void createPackage(String accountId, CreatePackageRequest request) throws PackageExistsException, UnauthorizedException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        switch (response.statusCode()) {
            case 401:
            case 403:
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
    public GetPackageResponse getPackage(String accountId, String name) throws PackageNotFoundException, UnauthorizedException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));

        RestGetPackageResponse response;
        HttpResponse<String> restResponse;
        try {
            HttpRequest request = getOAuthRequest("/account/" + accountId + "/package/" + name)
                    .GET()
                    .build();

            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        response = validateResponse(restResponse, name, RestGetPackageResponse.class);
        return response.toInternal();
    }

    @Override
    public PackageBuildsResponse getPackageBuilds(String accountId, ArchipelagoPackage pkg) throws PackageNotFoundException, UnauthorizedException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(pkg);

        RestPackageBuildsResponse response;
        HttpResponse<String> restResponse;
        try {
            HttpRequest request = getOAuthRequest("/account/" + accountId + "/package/" + pkg.getName() + "/" + pkg.getVersion())
                    .GET()
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
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
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(pkg);

        RestGetPackageBuildResponse response;
        HttpResponse<String> restResponse;
        try {
            HttpRequest request = getOAuthRequest("/account/" + accountId + "/package/" + pkg.getName() + "/" + pkg.getVersion() + "/" + pkg.getHash())
                    .GET()
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
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
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        response = validateResponse(restResponse, packageName + " (B: " + branch +", C:" + commit + ")",
                ArchipelagoBuiltPackageResponse.class);

        return new ArchipelagoBuiltPackage(response.getName(), response.getVersion(), response.getHash());
    }

    @Override
    public PackageVerificationResult<ArchipelagoPackage> verifyPackagesExists(String accountId, List<ArchipelagoPackage> packages) throws UnauthorizedException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        switch (restResponse.statusCode()) {
            case 401:
                throw new UnauthorizedException();
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
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
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
            case 401:
                throw new UnauthorizedException();
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
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        response = validateResponse(restResponse, request.getPkg().toString(), RestArtifactUploadResponse.class);
        return response.getHash();
    }

    @Override
    public Path getBuildArtifact(String accountId, ArchipelagoBuiltPackage pkg, Path directory) throws PackageNotFoundException, UnauthorizedException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
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

        GetBuildArtifactRestResponse restResponse;
        HttpResponse<String> restStringResponse;
        try {
            HttpRequest request = getOAuthRequest("/account/" + accountId + "/artifact/" +
                    pkg.getName() + "/" + pkg.getVersion() + "/" + pkg.getHash())
                    .GET()
                    .build();
            restStringResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        switch (restStringResponse.statusCode()) {
            case 401:
                throw new UnauthorizedException();
            case 404: // Not found
                throw new PackageNotFoundException(pkg);
            case 200: // Ok
                try {
                    restResponse = objectMapper.readValue(restStringResponse.body(), GetBuildArtifactRestResponse.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                throw new RuntimeException("Unknown response " + restStringResponse.statusCode());
        }

        HttpResponse<Path> restPathResponse;
        try {
            HttpRequest request = getOAuthRequest(restResponse.getUrl())
                    .GET()
                    .build();
            restPathResponse = client.send(request, HttpResponse.BodyHandlers.ofFile(filePath));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        switch (restPathResponse.statusCode()) {
            case 401:
                throw new UnauthorizedException();
            case 404: // Not found
                throw new PackageNotFoundException(pkg);
            case 200: // Ok
                return restPathResponse.body();
            default:
                throw new RuntimeException("Unknown response " + restPathResponse.statusCode());
        }
    }

    @Override
    public GetPackagesResponse getAllPackages(String accountId) throws UnauthorizedException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));

        HttpResponse<String> restResponse;
        try {
            HttpRequest request = getOAuthRequest("/account/" + accountId + "/package/all")
                    .GET()
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        switch (restResponse.statusCode()) {
            case 401:
                throw new UnauthorizedException();
            case 200: // Ok
                try {
                    RestGetPackagesResponse restObj = objectMapper.readValue(restResponse.body(),
                            RestGetPackagesResponse.class);
                    ImmutableList.Builder<GetPackageResponse> pkgs = ImmutableList.builder();
                    restObj.getPackages().forEach(x -> pkgs.add(x.toInternal()));
                    return GetPackagesResponse.builder().packages(pkgs.build()).build();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            default:
                throw new RuntimeException("Unknown response " + restResponse.statusCode());
        }
    }

    private <T> T validateResponse(HttpResponse<String> response, String packageName, Function<String, T> onOk) throws PackageNotFoundException, UnauthorizedException {

        switch (response.statusCode()) {
            case 401:
                throw new UnauthorizedException();
            case 404: // Not found
                throw new PackageNotFoundException(packageName);
            case 200: // Ok
                return onOk.apply(response.body());
            default:
                throw new RuntimeException("Unknown response " + response.statusCode());
        }
    }
    private <T> T validateResponse(HttpResponse<String> response, String packageName, Class<T> outputCalls) throws PackageNotFoundException, UnauthorizedException {
        return validateResponse(response, packageName, s -> {
            try {
                return objectMapper.readValue(response.body(), outputCalls);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
