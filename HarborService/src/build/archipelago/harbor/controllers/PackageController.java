package build.archipelago.harbor.controllers;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.*;
import build.archipelago.packageservice.models.CreatePackageRequest;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import com.google.common.base.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
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
    public void createPackage(@RequestBody CreatePackageRequest request) throws PackageExistsException {
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
    public build.archipelago.packageservice.models.GetPackageResponse getPackage(@PathVariable("name") String packageName) throws PackageNotFoundException {
        GetPackageResponse pkg = packageServiceClient.getPackage(accountId, packageName);


        return build.archipelago.packageservice.models.GetPackageResponse.builder()
                .name(pkg.getName())
                .description(pkg.getDescription())
                .created(pkg.getCreated().toEpochMilli())
                .versions(pkg.getVersions().stream().map(x -> new build.archipelago.packageservice.models.GetPackageResponse.Version(
                        x.getVersion(),
                        x.getLatestBuildHash(),
                        x.getLatestBuildTime().toEpochMilli())).collect(Collectors.toList()))
                .build();
    }
}
