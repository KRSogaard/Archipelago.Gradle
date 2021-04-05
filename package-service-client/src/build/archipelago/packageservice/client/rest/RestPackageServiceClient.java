package build.archipelago.packageservice.client.rest;

import build.archipelago.common.*;
import build.archipelago.common.clients.rest.*;
import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.git.models.GitBranch;
import build.archipelago.common.git.models.GitCommit;
import build.archipelago.common.git.models.exceptions.BranchNotFoundException;
import build.archipelago.common.git.models.exceptions.GitDetailsNotFound;
import build.archipelago.common.git.models.exceptions.RepoNotFoundException;
import build.archipelago.common.rest.models.errors.ProblemDetailRestResponse;
import build.archipelago.packageservice.client.*;
import build.archipelago.packageservice.client.models.*;
import build.archipelago.packageservice.exceptions.*;
import build.archipelago.packageservice.models.*;
import build.archipelago.packageservice.models.rest.*;
import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
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

        CreatePackageRestRequest restRequest = CreatePackageRestRequest.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        HttpResponse<String> restResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/account/" + accountId + "/package")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .header("accept", "application/json")
                    .build();
            restResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call package service to create a package");
            throw new RuntimeException(e);
        }

        switch (restResponse.statusCode()) {
            case 200: // Ok
                return;
            case 401:
            case 403:
                log.error("Got unauthorized response from Package service");
                throw new UnauthorizedException();
            case 409:
                log.warn("Got Conflict (409) response from Package service with body: " + restResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(restResponse.body());
                throw (PackageExistsException) PackageExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(restResponse);
        }
    }

    @Override
    public PackageDetails getPackage(String accountId, String name) throws PackageNotFoundException, UnauthorizedException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));

        GetPackageRestResponse response;
        HttpResponse<String> restResponse;
        try {
            HttpRequest request = this.getOAuthRequest("/account/" + accountId + "/package/" + name)
                    .GET()
                    .build();

            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call package service to get package '{}' for account '{}'", name, accountId);
            throw new RuntimeException(e);
        }
        response = this.validateResponse(restResponse, GetPackageRestResponse.class);
        return response.toInternal();
    }

    @Override
    public ImmutableList<VersionBuildDetails> getPackageBuilds(String accountId, ArchipelagoPackage pkg) throws PackageNotFoundException,
            UnauthorizedException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(pkg);

        GetPackageBuildsRestResponse response;
        HttpResponse<String> restResponse;
        try {
            HttpRequest request = this.getOAuthRequest("/account/" + accountId + "/package/" + pkg.getName() + "/" + pkg.getVersion())
                    .GET()
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call package service to get builds for package '{}' and account '{}'",
                    pkg.getNameVersion(), accountId);
            throw new RuntimeException(e);
        }
        response = this.validateResponse(restResponse, GetPackageBuildsRestResponse.class);
        return response.toInternal();
    }

    @Override
    public BuiltPackageDetails getPackageBuild(String accountId, ArchipelagoBuiltPackage pkg) throws PackageNotFoundException, UnauthorizedException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(pkg);

        GetPackageBuildRestResponse response;
        HttpResponse<String> restResponse;
        try {
            HttpRequest request = this.getOAuthRequest("/account/" + accountId + "/package/" + pkg.getName() + "/" + pkg.getVersion() + "/" + pkg.getHash())
                    .GET()
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call package service to package service build details for package '{}' and account '{}'",
                    pkg.getBuiltPackageName(), accountId);
            throw new RuntimeException(e);
        }
        response = this.validateResponse(restResponse, GetPackageBuildRestResponse.class);
        return response.toInternal();
    }

    @Override
    public ArchipelagoBuiltPackage getPackageByGit(String accountId, String packageName, String commit) throws PackageNotFoundException, UnauthorizedException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(packageName));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(commit));

        ArchipelagoBuiltPackageRestResponse response;
        HttpResponse<String> restResponse;
        try {
            HttpRequest request = this.getOAuthRequest("/account/" + accountId + "/package/" + packageName + "/git/" + commit)
                    .GET()
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call package service to get package {} by git commit '{}' and account '{}'",
                    packageName, commit, accountId);
            throw new RuntimeException(e);
        }
        response = this.validateResponse(restResponse, ArchipelagoBuiltPackageRestResponse.class);
        return response.toInternal();
    }

    @Override
    public PackageVerificationResult<ArchipelagoPackage> verifyPackagesExists(String accountId, List<ArchipelagoPackage> packages) throws UnauthorizedException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(packages.size() > 0);

        VerificationRestResponse response;
        HttpResponse<String> restResponse;
        try {
            VerificationRestRequest restRequest = VerificationRestRequest.from(packages);
            HttpRequest request = this.getOAuthRequest("/account/" + accountId + "/package/verify-packages")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call package service to verify package for account '{}'", accountId);
            throw new RuntimeException(e);
        }
        switch (restResponse.statusCode()) {
            case 401:
            case 403:
                log.error("Got unauthorized response from Package service");
                throw new UnauthorizedException();
            case 200: // Ok
                response = this.parseOrThrow(restResponse.body(), VerificationRestResponse.class);
                break;
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(restResponse);
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

        VerificationRestResponse response;
        HttpResponse<String> restResponse;
        try {
            VerificationRestRequest restRequest = VerificationRestRequest.fromBuilt(packages);
            HttpRequest request = this.getOAuthRequest("/account/" + accountId + "/package/verify-builds")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call package service verify builds for account '{}'", accountId);
            throw new RuntimeException(e);
        }
        switch (restResponse.statusCode()) {
            case 200: // Ok
                response = this.parseOrThrow(restResponse.body(), VerificationRestResponse.class);
                break;
            case 401:
            case 403:
                log.error("Got unauthorized response from Package service");
                throw new UnauthorizedException();
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(restResponse);
        }

        return PackageVerificationResult.<ArchipelagoBuiltPackage>builder()
                .missingPackages(ImmutableList.copyOf(
                        response.getMissing().stream().map(ArchipelagoBuiltPackage::parse).collect(Collectors.toList())))
                .build();
    }

    @Override
    public String uploadBuiltArtifact(String accountId, UploadPackageRequest uploadRequest, Path file) throws PackageNotFoundException, UnauthorizedException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(uploadRequest);
        Preconditions.checkNotNull(uploadRequest.getPkg());
        Preconditions.checkNotNull(file);
        Preconditions.checkArgument(Files.exists(file), "File did not exists");

        ArtifactUploadRestResponse response;
        HttpResponse<String> restResponse;
        try {
            String url = "/account/" + accountId + "/artifact/" + uploadRequest.getPkg().getName() + "/" + uploadRequest.getPkg().getVersion();
            ArtifactUploadRestRequest restRequest = ArtifactUploadRestRequest.builder()
                    .gitCommit(uploadRequest.getGitCommit())
                    .config(uploadRequest.getConfig())
                    .build();
            HttpRequest request = this.getOAuthRequest(url)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call package service to upload artifact for account '{}'", accountId);
            throw new RuntimeException(e);
        }
        response = this.validateResponse(restResponse, ArtifactUploadRestResponse.class);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(response.getUploadUrl()))
                    .PUT(HttpRequest.BodyPublishers.ofFile(file))
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (restResponse.statusCode() != 200) {
                throw new RuntimeException("Got response code: " + restResponse.statusCode());
            }
        } catch (Exception e) {
            log.error("Got unknown error while trying to upload artifact to S3 '{}'", accountId);
            throw new RuntimeException(e);
        }

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
            log.info("Creating directory '{}'", directory.toString());
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                log.error("Got unknown error when trying to create the directory '" + directory.toString() + "'", e);
                throw new RuntimeException(e);
            }
        }

        GetBuildArtifactResponse response = this.getBuildArtifact(accountId, pkg);

        log.debug("Got a signed url from package service to download the artifact for '{}', url '{}'",
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
    public GetBuildArtifactResponse getBuildArtifact(String accountId, ArchipelagoBuiltPackage pkg) throws PackageNotFoundException, UnauthorizedException {
        GetBuildArtifactRestResponse restResponse;
        HttpResponse<String> httpResponse;
        try {
            HttpRequest request = this.getOAuthRequest("/account/" + accountId + "/artifact/" +
                    pkg.getName() + "/" + pkg.getVersion() + "/" + pkg.getHash())
                    .GET()
                    .build();
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call package service to get download url for artifact '{}' account '{}'",
                    pkg.getBuiltPackageName(), accountId);
            throw new RuntimeException(e);
        }
        switch (httpResponse.statusCode()) {
            case 200: // Ok
                restResponse = this.parseOrThrow(httpResponse.body(), GetBuildArtifactRestResponse.class);
                return restResponse.toInternal();
            case 401:
            case 403:
                log.error("Got unauthorized response from Package service");
                throw new UnauthorizedException();
            case 404: // Not found
                log.warn("Got Not Found (404) response from Package service with body: " + httpResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(httpResponse.body());
                throw (PackageNotFoundException) PackageExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }
    }

    @Override
    public ImmutableList<PackageDetails> getAllPackages(String accountId) throws UnauthorizedException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));

        HttpResponse<String> restResponse;
        try {
            HttpRequest request = this.getOAuthRequest("/account/" + accountId + "/package")
                    .GET()
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call package service to get all packages for account '{}'", accountId);
            throw new RuntimeException(e);
        }
        switch (restResponse.statusCode()) {
            case 200: // Ok
                return this.parseOrThrow(restResponse.body(), GetPackagesRestResponse.class)
                        .getPackages().stream().map(GetPackageRestResponse::toInternal)
                        .collect(ImmutableList.toImmutableList());
            case 401:
            case 403:
                log.error("Got unauthorized response from Package service");
                throw new UnauthorizedException();
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(restResponse);
        }
    }

    @Override
    public ImmutableList<GitBranch> getGitBranches(String accountId, String pkgName) throws PackageNotFoundException, RepoNotFoundException, GitDetailsNotFound {
        GitBranchesRestResponse restResponse;
        HttpResponse<String> httpResponse;
        try {
            HttpRequest request = this.getOAuthRequest("/account/" + accountId + "/git/" + pkgName + "/branches")
                    .GET()
                    .build();
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call package service to get git branches for package '{}' account '{}'",
                    pkgName, accountId);
            throw new RuntimeException(e);
        }
        switch (httpResponse.statusCode()) {
            case 200: // Ok
                restResponse = this.parseOrThrow(httpResponse.body(), GitBranchesRestResponse.class);
                return restResponse.getBranches().stream()
                        .map(GitBranchRestResponse::toInternal)
                        .collect(ImmutableList.toImmutableList());
            case 401:
            case 403:
                log.error("Got unauthorized response from Package service");
                throw new UnauthorizedException();
            case 400:
                Exception exp = PackageExceptionHandler.createException(
                        ProblemDetailRestResponse.from(httpResponse.body()));
                if (exp instanceof RepoNotFoundException) {
                    throw (RepoNotFoundException) exp;
                } else if (exp instanceof GitDetailsNotFound) {
                    throw (GitDetailsNotFound) exp;
                } else {
                    throw new RuntimeException("Unknown issue");
                }
            case 404: // Not found
                log.warn("Got Not Found (404) response from Package service with body: " + httpResponse.body());
                throw (PackageNotFoundException) PackageExceptionHandler.createException(
                        ProblemDetailRestResponse.from(httpResponse.body()));
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }
    }

    @Override
    public ImmutableList<GitCommit> getGitCommits(String accountId, String pkgName, String branch) throws RepoNotFoundException, BranchNotFoundException, GitDetailsNotFound, PackageNotFoundException {
        GitCommitsRestResponse restResponse;
        HttpResponse<String> httpResponse;
        try {
            HttpRequest request = this.getOAuthRequest("/account/" + accountId + "/git/" + pkgName + "/" + branch)
                    .GET()
                    .build();
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call package service to get git branches for package '{}' account '{}'",
                    pkgName, accountId);
            throw new RuntimeException(e);
        }
        switch (httpResponse.statusCode()) {
            case 200: // Ok
                restResponse = this.parseOrThrow(httpResponse.body(), GitCommitsRestResponse.class);
                return ImmutableList.copyOf(restResponse.getCommits().stream().map(GitCommitRestResponse::toInternal).collect(Collectors.toList()));
            case 401:
            case 403:
                log.error("Got unauthorized response from Package service");
                throw new UnauthorizedException();
            case 400:
                Exception exp = PackageExceptionHandler.createException(
                        ProblemDetailRestResponse.from(httpResponse.body()));
                if (exp instanceof RepoNotFoundException) {
                    throw (RepoNotFoundException) exp;
                } else if (exp instanceof BranchNotFoundException) {
                    throw (BranchNotFoundException) exp;
                } else if (exp instanceof GitDetailsNotFound) {
                    throw (GitDetailsNotFound) exp;
                } else {
                    throw new RuntimeException("Unknown issue");
                }
            case 404: // Not found
                log.warn("Got Not Found (404) response from Package service with body: " + httpResponse.body());
                throw (PackageNotFoundException) PackageExceptionHandler.createException(
                        ProblemDetailRestResponse.from(httpResponse.body()));
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(httpResponse);
        }
    }

    private <T> T validateResponse(HttpResponse<String> response, Function<String, T> onOk) throws PackageNotFoundException, UnauthorizedException {
        switch (response.statusCode()) {
            case 200: // Ok
                return onOk.apply(response.body());
            case 401:
            case 403:
                log.error("Got unauthorized response from Package service");
                throw new UnauthorizedException();
            case 404: // Not found
                log.warn("Got Not Found (404) response from Package service with body: " + response.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(response.body());
                throw (PackageNotFoundException) PackageExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(response);
        }
    }

    private <T> T validateResponse(HttpResponse<String> response, Class<T> outputCalls) throws PackageNotFoundException, UnauthorizedException {
        return this.validateResponse(response, s -> this.parseOrThrow(response.body(), outputCalls));
    }

    private RuntimeException logAndReturnExceptionForUnknownStatusCode(HttpResponse<String> restResponse) {
        log.error("Unknown response from package service status code " + restResponse.statusCode() +
                " body: " + restResponse.body());
        return new RuntimeException("Unknown response from package service status code " + restResponse.statusCode() +
                " body: " + restResponse.body());
    }
}
