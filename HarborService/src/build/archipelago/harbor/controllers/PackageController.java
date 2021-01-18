package build.archipelago.harbor.controllers;

import build.archipelago.common.*;
import build.archipelago.harbor.filters.AccountIdFilter;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.exceptions.*;
import build.archipelago.packageservice.models.*;
import build.archipelago.packageservice.models.rest.*;
import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

@RestController
@RequestMapping("packages")
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
    public GetBuildArtifactRestResponse getBuildArtifact(@PathVariable("name") String name,
                                                         @PathVariable("version") String version,
                                                         @PathVariable("hash") String hash) throws PackageNotFoundException, IOException {
        log.info("Request to get build artifact for Package {}, Version: {}, Hash: {}", name, version, hash);
        ArchipelagoPackage pkg = new ArchipelagoPackage(name, version);

        GetBuildArtifactResponse downloadResponse = packageServiceClient.getBuildArtifact(accountId, new ArchipelagoBuiltPackage(name, version,
                hash));
        return GetBuildArtifactRestResponse.from(downloadResponse);
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
        BuiltPackageDetails response = packageServiceClient.getPackageBuild(
                accountId, new ArchipelagoBuiltPackage(name, version, hash));

        return response.getConfig();
    }

    @GetMapping("{name}")
    public GetPackageRestResponse getPackage(@PathVariable("name") String packageName,
                                             @RequestAttribute(AccountIdFilter.Key) String accountId) throws PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));

        PackageDetails pkg = packageServiceClient.getPackage(accountId, packageName);
        return GetPackageRestResponse.from(pkg);
    }

    @GetMapping("all")
    public GetPackagesRestResponse getAllPackages(@RequestAttribute(AccountIdFilter.Key) String accountId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));

        ImmutableList<PackageDetails> packages = packageServiceClient.getAllPackages(accountId);
        return GetPackagesRestResponse.builder()
                .packages(packages.stream().map(GetPackageRestResponse::from).collect(Collectors.toList()))
                .build();
    }
}
