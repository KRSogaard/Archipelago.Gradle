package build.archipelago.packageservice.controllers;

import build.archipelago.account.common.exceptions.GitDetailsNotFound;
import build.archipelago.common.*;
import build.archipelago.packageservice.core.delegates.createPackage.*;
import build.archipelago.packageservice.core.delegates.getPackage.GetPackageDelegate;
import build.archipelago.packageservice.core.delegates.getPackageBuild.GetPackageBuildDelegate;
import build.archipelago.packageservice.core.delegates.getPackageBuildByGit.GetPackageBuildByGitDelegate;
import build.archipelago.packageservice.core.delegates.getPackageBuilds.GetPackageBuildsDelegate;
import build.archipelago.packageservice.core.delegates.getPackages.GetPackagesDelegate;
import build.archipelago.packageservice.core.delegates.verifyBuildsExists.VerifyBuildsExistsDelegate;
import build.archipelago.packageservice.core.delegates.verifyPackageExists.VerifyPackageExistsDelegate;
import build.archipelago.packageservice.exceptions.*;
import build.archipelago.packageservice.models.*;
import build.archipelago.packageservice.models.rest.*;
import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("account/{accountId}/package")
@Slf4j
@CrossOrigin(origins = "*")
public class PackagesController {

    private CreatePackageDelegate createPackageDelegate;
    private GetPackageDelegate getPackageDelegate;
    private GetPackageBuildsDelegate getPackageBuildsDelegate;
    private GetPackageBuildDelegate getPackageBuildDelegate;
    private GetPackageBuildByGitDelegate getPackageBuildByGitDelegate;
    private VerifyBuildsExistsDelegate verifyBuildsExistsDelegate;
    private VerifyPackageExistsDelegate verifyPackageExistsDelegate;
    private GetPackagesDelegate getPackagesDelegate;

    public PackagesController(GetPackageDelegate getPackageDelegate,
                              CreatePackageDelegate createPackageDelegate,
                              GetPackageBuildsDelegate getPackageBuildsDelegate,
                              GetPackageBuildDelegate getPackageBuildDelegate,
                              GetPackageBuildByGitDelegate getPackageBuildByGitDelegate,
                              VerifyBuildsExistsDelegate verifyBuildsExistsDelegate,
                              VerifyPackageExistsDelegate verifyPackageExistsDelegate,
                              GetPackagesDelegate getPackagesDelegate) {
        Preconditions.checkNotNull(createPackageDelegate);
        this.createPackageDelegate = createPackageDelegate;
        Preconditions.checkNotNull(getPackageDelegate);
        this.getPackageDelegate = getPackageDelegate;
        Preconditions.checkNotNull(getPackageBuildsDelegate);
        this.getPackageBuildsDelegate = getPackageBuildsDelegate;
        Preconditions.checkNotNull(getPackageBuildDelegate);
        this.getPackageBuildDelegate = getPackageBuildDelegate;
        Preconditions.checkNotNull(verifyBuildsExistsDelegate);
        this.verifyBuildsExistsDelegate = verifyBuildsExistsDelegate;
        Preconditions.checkNotNull(verifyPackageExistsDelegate);
        this.verifyPackageExistsDelegate = verifyPackageExistsDelegate;
        Preconditions.checkNotNull(getPackageBuildByGitDelegate);
        this.getPackageBuildByGitDelegate = getPackageBuildByGitDelegate;
        Preconditions.checkNotNull(getPackagesDelegate);
        this.getPackagesDelegate = getPackagesDelegate;
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.OK)
    public void createPackage(
            @PathVariable("accountId") String accountId,
            @RequestBody CreatePackageRestRequest request) throws PackageExistsException, GitDetailsNotFound {
        log.info("Request to create package {} for account {}", request.getName(), accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(request);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getName()));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getDescription()));

        createPackageDelegate.create(CreatePackageDelegateRequest.builder()
                .accountId(accountId)
                .name(request.getName())
                .description(request.getDescription())
                .build());
    }


    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public GetPackagesRestResponse getAllPackages(
            @PathVariable("accountId") String accountId) {
        log.info("Request to get packages for account {}", accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));

        ImmutableList<PackageDetails> packages = getPackagesDelegate.get(accountId);
        return GetPackagesRestResponse.builder()
                .packages(packages.stream().map(GetPackageRestResponse::from).collect(Collectors.toList()))
                .build();
    }

    @GetMapping(value = "{name}")
    @ResponseStatus(HttpStatus.OK)
    public GetPackageRestResponse getPackage(
            @PathVariable("accountId") String accountId,
            @PathVariable("name") String name
    ) throws PackageNotFoundException {
        log.debug("Request to get information on package {} for account {}", name, accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));

        PackageDetails pkg = getPackageDelegate.get(accountId, name);
        return GetPackageRestResponse.from(pkg);
    }

    @GetMapping(value = "{name}/{version}")
    @ResponseStatus(HttpStatus.OK)
    public GetPackageBuildsRestResponse getPackageBuilds(
            @PathVariable("accountId") String accountId,
            @PathVariable("name") String name,
            @PathVariable("version") String version
    ) {
        log.info("Request to get package builds for {}-{} for account {}", name, version, accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(version));
        ArchipelagoPackage pkg = new ArchipelagoPackage(name, version);
        log.info("Request to get builds for {}", pkg);

        ImmutableList<VersionBuildDetails> builds = getPackageBuildsDelegate.get(accountId, pkg);

        return GetPackageBuildsRestResponse.from(builds);
    }

    @GetMapping(value = "{name}/{version}/{hash}")
    @ResponseStatus(HttpStatus.OK)
    public GetPackageBuildRestResponse getPackageBuild(
            @PathVariable("accountId") String accountId,
            @PathVariable("name") String name,
            @PathVariable("version") String version,
            @PathVariable("hash") String hash
    ) throws PackageNotFoundException {
        log.info("Request to get package build for {}-{}#{} for account {}", name, version, hash, accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(version));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(hash));
        ArchipelagoBuiltPackage pkg = new ArchipelagoBuiltPackage(name, version, hash);
        log.info("Request to get build details for {}", pkg);

        BuiltPackageDetails build = getPackageBuildDelegate.get(accountId, pkg);
        return GetPackageBuildRestResponse.from(build);
    }

    @GetMapping(value = "{name}/git/{commit}")
    @ResponseStatus(HttpStatus.OK)
    public ArchipelagoBuiltPackageRestResponse getPackageByGit(
            @PathVariable("accountId") String accountId,
            @PathVariable("name") String name,
            @PathVariable("commit") String commit) throws PackageNotFoundException {
        log.info("Request to get package build by git {} (C: {}) for account {}", name, commit, accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(commit);

        ArchipelagoBuiltPackage pkg = getPackageBuildByGitDelegate.get(accountId, name, commit);
        return ArchipelagoBuiltPackageRestResponse.from(pkg);
    }

    @PostMapping(value = "verify-packages")
    @ResponseStatus(HttpStatus.OK)
    public VerificationRestResponse verifyPackages(
            @PathVariable("accountId") String accountId,
            @RequestBody VerificationRestRequest request) {
        log.info("Request to get verify {} packages for account {}", request.getPackages().size(), accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        ImmutableList.Builder<ArchipelagoPackage> packages = ImmutableList.builder();
        for (String pkg : request.getPackages()) {
            packages.add(ArchipelagoPackage.parse(pkg));
        }
        ImmutableList<ArchipelagoPackage> pkgs = packages.build();
        log.info("Verifying packages for: {}", pkgs);

        ImmutableList<ArchipelagoPackage> missing = verifyPackageExistsDelegate.verify(accountId, pkgs);
        return VerificationRestResponse.builder()
                .missing(missing.stream().map(ArchipelagoPackage::getNameVersion).collect(Collectors.toList()))
                .build();
    }

    @PostMapping(value = "verify-builds")
    @ResponseStatus(HttpStatus.OK)
    public VerificationRestResponse verifyBuilds(
            @PathVariable("accountId") String accountId,
            @RequestBody VerificationRestRequest request) {
        log.info("Request to get verify {} builds for account {}", request.getPackages().size(), accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        ImmutableList.Builder<ArchipelagoBuiltPackage> packages = ImmutableList.builder();
        for (String pkg : request.getPackages()) {
            packages.add(ArchipelagoBuiltPackage.parse(pkg));
        }
        ImmutableList<ArchipelagoBuiltPackage> pkgs = packages.build();
        log.info("Verifying builds for: {}", pkgs);

        ImmutableList<ArchipelagoBuiltPackage> missing = verifyBuildsExistsDelegate.verify(accountId, pkgs);
        return VerificationRestResponse.builder()
                .missing(missing.stream().map(ArchipelagoBuiltPackage::toString).collect(Collectors.toList()))
                .build();
    }
}
