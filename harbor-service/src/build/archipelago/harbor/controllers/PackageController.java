package build.archipelago.harbor.controllers;

import build.archipelago.common.*;
import build.archipelago.common.git.models.GitBranch;
import build.archipelago.common.git.models.GitCommit;
import build.archipelago.common.git.models.exceptions.BranchNotFoundException;
import build.archipelago.common.git.models.exceptions.GitDetailsNotFound;
import build.archipelago.common.git.models.exceptions.RepoNotFoundException;
import build.archipelago.harbor.filters.AccountIdFilter;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.exceptions.*;
import build.archipelago.packageservice.models.*;
import build.archipelago.packageservice.models.rest.*;
import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("packages")
@Slf4j
@CrossOrigin(origins = "*")
public class PackageController {
    private PackageServiceClient packageServiceClient;

    public PackageController(PackageServiceClient packageServiceClient) {
        Preconditions.checkNotNull(packageServiceClient);
        this.packageServiceClient = packageServiceClient;
    }

    @GetMapping(value = {"{name}/{version}/{hash}/artifact"})
    public GetBuildArtifactRestResponse getBuildArtifact(
            @RequestAttribute(AccountIdFilter.AccountIdKey) String accountId,
            @PathVariable("name") String name,
            @PathVariable("version") String version,
            @PathVariable("hash") String hash) throws PackageNotFoundException, IOException {
        log.info("Request to get build artifact for Package '{}', Version: '{}', Hash: '{}'", name, version, hash);
        ArchipelagoPackage pkg = new ArchipelagoPackage(name, version);

        GetBuildArtifactResponse downloadResponse = packageServiceClient.getBuildArtifact(accountId, new ArchipelagoBuiltPackage(name, version,
                hash));
        return GetBuildArtifactRestResponse.from(downloadResponse);
    }

    @PostMapping()
    public void createPackage(
            @RequestAttribute(AccountIdFilter.AccountIdKey) String accountId,
            @RequestBody CreatePackageRestRequest request) throws PackageExistsException {
        log.info("Request to create package '{}' for account '{}'", request.getName(), accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(request);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getName()));

        packageServiceClient.createPackage(accountId, build.archipelago.packageservice.client.models.CreatePackageRequest.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build());
    }

    @GetMapping("{name}/{version}/{hash}/config")
    public String getConfig(
            @RequestAttribute(AccountIdFilter.AccountIdKey) String accountId,
            @PathVariable("name") String name,
            @PathVariable("version") String version,
            @PathVariable("hash") String hash) throws PackageNotFoundException {
        log.info("Request to get config for package '{}-{}#{}' for account '{}'", name, version, hash, accountId);
        BuiltPackageDetails response = packageServiceClient.getPackageBuild(
                accountId, new ArchipelagoBuiltPackage(name, version, hash));

        return response.getConfig();
    }

    @GetMapping("{name}")
    public GetPackageRestResponse getPackage(
            @RequestAttribute(AccountIdFilter.AccountIdKey) String accountId,
            @PathVariable("name") String packageName) throws PackageNotFoundException {
        log.info("Request to get package '{}' for account '{}'", packageName, accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));

        PackageDetails pkg = packageServiceClient.getPackage(accountId, packageName);
        return GetPackageRestResponse.from(pkg);
    }

    @GetMapping("all")
    public GetPackagesRestResponse getAllPackages(@RequestAttribute(AccountIdFilter.AccountIdKey) String accountId) {
        log.info("Request to get all packages for account '{}'", accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));

        ImmutableList<PackageDetails> packages = packageServiceClient.getAllPackages(accountId);
        return GetPackagesRestResponse.builder()
                .packages(packages.stream().map(GetPackageRestResponse::from).collect(Collectors.toList()))
                .build();
    }

    @GetMapping(value = "{name}/branches")
    @ResponseStatus(HttpStatus.OK)
    public GitBranchesRestResponse getPackageBranches(
            @RequestAttribute(AccountIdFilter.AccountIdKey) String accountId,
            @PathVariable("name") String name
    ) throws PackageNotFoundException, GitDetailsNotFound, RepoNotFoundException {
        log.info("Request to get git branches for package {} on account {}", name, accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));

        List<GitBranch> branches = packageServiceClient.getGitBranches(accountId, name);
        return GitBranchesRestResponse.builder()
                .branches(branches.stream().map(GitBranchRestResponse::from).collect(Collectors.toList()))
                .build();
    }

    @GetMapping(value = "{name}/{branch}/commits")
    @ResponseStatus(HttpStatus.OK)
    public GitCommitsRestResponse getPackageCommits(
            @RequestAttribute(AccountIdFilter.AccountIdKey) String accountId,
            @PathVariable("name") String name,
            @PathVariable("branch") String branch
    ) throws PackageNotFoundException, GitDetailsNotFound, RepoNotFoundException, BranchNotFoundException {
        log.info("Request to get git commits for package {}, branch {} on account {}", name, branch, accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));

        List<GitCommit> commits = packageServiceClient.getGitCommits(accountId, name, branch);
        return GitCommitsRestResponse.builder()
                .commits(commits.stream().map(GitCommitRestResponse::from).collect(Collectors.toList()))
                .build();
    }
}
