package build.archipelago.packageservice.client.rest;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.*;
import build.archipelago.packageservice.client.rest.models.*;
import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.*;
import org.springframework.http.*;
import org.springframework.util.*;
import org.springframework.web.client.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RestPackageServiceClient implements PackageServiceClient {

    private RestTemplate restTemplate;
    private String endpoint;
    private WebClient webClient;

    public RestPackageServiceClient(String endpoint) {
        restTemplate = new RestTemplate();
        if (endpoint.endsWith("/")) {
            this.endpoint = endpoint.substring(0, endpoint.length() - 2);
        } else {
            this.endpoint = endpoint;
        }

        webClient = WebClient.builder()
                .baseUrl(endpoint)
                .build();
    }

    public void useInternalAuthentication(String accountId) {
        webClient = WebClient.builder()
                .baseUrl(endpoint)
                .defaultHeader(ClientConstants.HEADER_ACCOUNT_ID, accountId)
                .build();
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

        try {
            webClient.post()
                    .uri("/package")
                    .body(Mono.just(restRequest), RestCreatePackageRequest.class)
                    .retrieve()
                    .onStatus(HttpStatus.CONFLICT::equals, response -> Mono.error(new PackageExistsException(request.getName())))
                    .bodyToMono(String.class)
                    .block();
        } catch (RuntimeException exp) {
            if (exp.getCause() != null && exp.getCause().getClass().equals(PackageExistsException.class)) {
                throw (PackageExistsException)exp.getCause();
            }
            throw exp;
        }
    }

    @Override
    public GetPackageResponse getPackage(String name) throws PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));

        RestGetPackageResponse response;
        try {
            response = webClient.get()
                    .uri("/package/" + name)
                    .retrieve()
                    .onStatus(HttpStatus.NOT_FOUND::equals, r -> Mono.error(new PackageNotFoundException(name)))
                    .bodyToMono(RestGetPackageResponse.class)
                    .block();
        } catch (RuntimeException exp) {
            if (exp.getCause() != null && exp.getCause().getClass().equals(PackageNotFoundException.class)) {
                throw (PackageNotFoundException)exp.getCause();
            }
            throw exp;
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

    @Override
    public PackageBuildsResponse getPackageBuilds(ArchipelagoPackage pkg) throws PackageNotFoundException {
        Preconditions.checkNotNull(pkg);


        RestPackageBuildsResponse response;
        try {
            response = webClient.get()
                    .uri("/package/" + pkg.getName() + "/" + pkg.getVersion())
                    .retrieve()
                    .onStatus(HttpStatus.NOT_FOUND::equals, r -> Mono.error(new PackageNotFoundException(pkg)))
                    .bodyToMono(RestPackageBuildsResponse.class)
                    .block();
        } catch (RuntimeException exp) {
            if (exp.getCause() != null && exp.getCause().getClass().equals(PackageNotFoundException.class)) {
                throw (PackageNotFoundException)exp.getCause();
            }
            throw exp;
        }

        ImmutableList.Builder<PackageBuildsResponse.Build> builds = ImmutableList.builder();
        response.getBuilds().forEach(x -> builds.add(new PackageBuildsResponse.Build(
                x.getHash(), Instant.ofEpochMilli(x.getCreated())
        )));

        return PackageBuildsResponse.builder()
                .builds(builds.build())
                .build();
    }

    @Override
    public GetPackageBuildResponse getPackageBuild(ArchipelagoBuiltPackage pkg) throws PackageNotFoundException {
        Preconditions.checkNotNull(pkg);

        RestGetPackageBuildResponse response;
        try {
            response = webClient.get()
                    .uri("/package/" + pkg.getName() + "/" + pkg.getVersion() + "/" + pkg.getHash())
                    .retrieve()
                    .onStatus(HttpStatus.NOT_FOUND::equals, r -> Mono.error(new PackageNotFoundException(pkg)))
                    .bodyToMono(RestGetPackageBuildResponse.class)
                    .block();
        } catch (RuntimeException exp) {
            if (exp.getCause() != null && exp.getCause().getClass().equals(PackageNotFoundException.class)) {
                throw (PackageNotFoundException)exp.getCause();
            }
            throw exp;
        }

        return GetPackageBuildResponse.builder()
                .hash(response.getHash())
                .created(Instant.ofEpochMilli(response.getCreated()))
                .config(response.getConfig())
                .gitCommit(response.getGitCommit())
                .gitBranch(response.getGitBranch())
                .build();
    }

    @Override
    public ArchipelagoBuiltPackage getPackageByGit(String packageName, String branch, String commit) throws PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(packageName));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(branch));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(commit));

        ArchipelagoBuiltPackageResponse response;
        try {
            response = webClient.get()
                    .uri("/package/" + packageName + "/git/" + branch + "/" + commit)
                    .retrieve()
                    .onStatus(HttpStatus.NOT_FOUND::equals, r -> Mono.error(new PackageNotFoundException(packageName)))
                    .bodyToMono(ArchipelagoBuiltPackageResponse.class)
                    .block();
        } catch (RuntimeException exp) {
            if (exp.getCause() != null && exp.getCause().getClass().equals(PackageNotFoundException.class)) {
                throw (PackageNotFoundException)exp.getCause();
            }
            throw exp;
        }

        return new ArchipelagoBuiltPackage(response.getName(), response.getVersion(), response.getHash());
    }

    @Override
    public PackageVerificationResult<ArchipelagoPackage> verifyPackagesExists(List<ArchipelagoPackage> packages) {
        Preconditions.checkNotNull(packages);
        Preconditions.checkArgument(packages.size() > 0);

        RestVerificationRequest restRequest = new RestVerificationRequest(
                packages.stream().map(ArchipelagoPackage::getNameVersion).collect(Collectors.toList()));

        try {
            RestVerificationResponse res = restTemplate.postForObject(endpoint + "/package/verify-packages",
                    restRequest, RestVerificationResponse.class);

            return PackageVerificationResult.<ArchipelagoPackage>builder()
                    .missingPackages(ImmutableList.copyOf(
                            res.getMissing().stream().map(ArchipelagoPackage::parse).collect(Collectors.toList())))
                    .build();
        } catch (HttpClientErrorException exp) {
            throw new RuntimeException("Was unable to verify packages", exp);
        }
    }

    @Override
    public PackageVerificationResult<ArchipelagoBuiltPackage> verifyBuildsExists(List<ArchipelagoBuiltPackage> packages) {
        Preconditions.checkNotNull(packages);
        Preconditions.checkArgument(packages.size() > 0);

        RestVerificationRequest restRequest = new RestVerificationRequest(
                packages.stream().map(ArchipelagoBuiltPackage::toString).collect(Collectors.toList()));

        RestVerificationResponse response = webClient.post()
                .uri("/package/verify-builds")
                .body(Mono.just(restRequest), RestVerificationRequest.class)
                .retrieve()
                .bodyToMono(RestVerificationResponse.class)
                .block();

        return PackageVerificationResult.<ArchipelagoBuiltPackage>builder()
                .missingPackages(ImmutableList.copyOf(
                        response.getMissing().stream().map(ArchipelagoBuiltPackage::parse).collect(Collectors.toList())))
                .build();
    }

    @Override
    public String uploadBuiltArtifact(UploadPackageRequest request, Path file) throws PackageNotFoundException {
        Preconditions.checkNotNull(request);
        Preconditions.checkNotNull(request.getPkg());
        Preconditions.checkNotNull(file);
        Preconditions.checkArgument(Files.exists(file), "File did not exists");


        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("buildArtifact", new FileSystemResource(file));
        body.add("config", request.getConfig());
        body.add("gitCommit", request.getGitCommit());
        body.add("gitBranch", request.getGitBranch());

        RestArtifactUploadResponse response;
        try {
            response = webClient.post()
                    .uri("/artifact/" + request.getPkg().getName() + "/" + request.getPkg().getVersion())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .onStatus(HttpStatus.NOT_FOUND::equals, r -> Mono.error(new PackageNotFoundException(request.getPkg())))
                    .bodyToMono(RestArtifactUploadResponse.class)
                    .block();
        } catch (RuntimeException exp) {
            if (exp.getCause() != null && exp.getCause().getClass().equals(PackageNotFoundException.class)) {
                throw (PackageNotFoundException)exp.getCause();
            }
            throw exp;
        }
        return response.getHash();
    }

    @Override
    public Path getBuildArtifact(ArchipelagoBuiltPackage pkg, Path directory) throws PackageNotFoundException, IOException {
        Preconditions.checkNotNull(pkg, "Name and Version is required");
        Preconditions.checkNotNull(directory, "A save location is required");

        Path filePath = Paths.get(
                directory.toString(),
                String.format("%s.zip", java.util.UUID.randomUUID().toString()));

        if (!Files.isDirectory(directory)) {
            log.info("Creating directory \"%s\"", directory.toString());
            Files.createDirectories(directory);
        }

        try {
            final Flux<DataBuffer> dataBufferFlux = webClient.get()
                    .uri("/artifact/" + pkg.getName() + "/" + pkg.getVersion() + "/" + pkg.getHash())
                    .retrieve()
                    .onStatus(HttpStatus.NOT_FOUND::equals, r -> Mono.error(new PackageNotFoundException(pkg)))
                    .bodyToFlux(DataBuffer.class);
            DataBufferUtils
                    .write(dataBufferFlux, filePath, StandardOpenOption.CREATE_NEW)
                    .block();

            return filePath;
        } catch (RuntimeException exp) {
            if (exp.getCause() != null && exp.getCause().getClass().equals(PackageNotFoundException.class)) {
                throw (PackageNotFoundException)exp.getCause();
            }
            throw exp;
        }
    }
}
