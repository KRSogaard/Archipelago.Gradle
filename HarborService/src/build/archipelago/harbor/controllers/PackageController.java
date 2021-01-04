package build.archipelago.harbor.controllers;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.PackageExistsException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.harbor.filters.AccountIdFilter;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.GetPackageBuildResponse;
import build.archipelago.packageservice.client.models.GetPackageResponse;
import build.archipelago.packageservice.client.models.GetPackagesResponse;
import build.archipelago.packageservice.models.CreatePackageRestRequest;
import build.archipelago.packageservice.models.GetPackageRestResponse;
import build.archipelago.packageservice.models.GetPackagesRestResponse;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("package")
@Slf4j
@CrossOrigin(origins = "*")
public class PackageController {

    private String accountId = "wewelo";
    private Path tempdir;
    private PackageServiceClient packageServiceClient;

    public PackageController(PackageServiceClient packageServiceClient,
                            @Qualifier("tempDir") Path tempdir) {
        Preconditions.checkNotNull(packageServiceClient);
        this.packageServiceClient = packageServiceClient;
        this.tempdir = tempdir;
    }

    @GetMapping(value = {"{name}/{version}/{hash}/artifact"})
    public ResponseEntity<Resource> getBuildArtifact(@PathVariable("name") String name,
                                                     @PathVariable("version") String version,
                                                     @PathVariable("hash") String hash) throws PackageNotFoundException, IOException {
        log.info("Request to get build artifact for Package {}, Version: {}, Hash: {}", name, version, hash);
        ArchipelagoPackage pkg = new ArchipelagoPackage(name, version);

        Path file = packageServiceClient.getBuildArtifact(accountId, new ArchipelagoBuiltPackage(name, version, hash), tempdir);

        try {
            String zipFileName = String.format("%s.zip", pkg.toString());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFileName + "\"")
                    .body(new ByteArrayResource(Files.readAllBytes(file)));
        } finally {
            if (Files.exists(file)) {
                Files.delete(file);
            }
        }
    }

    @PostMapping()
    public void createPackage(@RequestBody CreatePackageRestRequest request) throws PackageExistsException {
        log.info("Request to create package {} for account {}", request.getName(), accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(request);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getName()));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getDescription()));

        packageServiceClient.createPackage(accountId, build.archipelago.packageservice.client.models.CreatePackageRequest.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build());
    }

    @GetMapping("{name}/{version}/{hash}/config")
    public String getConfig(@PathVariable("name") String name,
                            @PathVariable("version") String version,
                            @PathVariable("hash") String hash) throws PackageNotFoundException {
        GetPackageBuildResponse response = packageServiceClient.getPackageBuild(
                accountId, new ArchipelagoBuiltPackage(name, version, hash));

        return response.getConfig();
    }

    @GetMapping("{name}")
    public GetPackageRestResponse getPackage(@PathVariable("name") String packageName,
                                             @RequestAttribute(AccountIdFilter.Key) String accountId) throws PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));

        GetPackageResponse pkg = packageServiceClient.getPackage(accountId, packageName);

        return GetPackageRestResponse.builder()
                .name(pkg.getName())
                .description(pkg.getDescription())
                .created(pkg.getCreated().toEpochMilli())
                .versions(pkg.getVersions().stream().map(x -> new GetPackageRestResponse.VersionRestResponse(
                        x.getVersion(),
                        x.getLatestBuildHash(),
                        x.getLatestBuildTime().toEpochMilli())).collect(Collectors.toList()))
                .build();
    }

    @GetMapping("all")
    public GetPackagesRestResponse getAllPackages(@RequestAttribute(AccountIdFilter.Key) String accountId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));

        GetPackagesResponse packages = packageServiceClient.getAllPackages(accountId);
        List<GetPackageRestResponse> pkgList = new ArrayList<>();
        for (GetPackageResponse pkg : packages.getPackages()) {
            pkgList.add(GetPackageRestResponse.builder()
                    .name(pkg.getName())
                    .description(pkg.getDescription())
                    .created(pkg.getCreated().toEpochMilli())
                    .versions(pkg.getVersions().stream().map(x -> new GetPackageRestResponse.VersionRestResponse(
                            x.getVersion(),
                            x.getLatestBuildHash(),
                            x.getLatestBuildTime().toEpochMilli())).collect(Collectors.toList()))
                    .build());
        }
        return GetPackagesRestResponse.builder()
                .packages(pkgList)
                .build();
    }
}
