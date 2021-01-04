package build.archipelago.packageservice.controllers;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.PackageExistsException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.core.data.models.BuiltPackageDetails;
import build.archipelago.packageservice.core.data.models.PackageDetails;
import build.archipelago.packageservice.core.data.models.VersionBuildDetails;
import build.archipelago.packageservice.core.delegates.createPackage.CreatePackageDelegate;
import build.archipelago.packageservice.core.delegates.createPackage.CreatePackageDelegateRequest;
import build.archipelago.packageservice.core.delegates.getPackage.GetPackageDelegate;
import build.archipelago.packageservice.core.delegates.getPackageBuild.GetPackageBuildDelegate;
import build.archipelago.packageservice.core.delegates.getPackageBuildByGit.GetPackageBuildByGitDelegate;
import build.archipelago.packageservice.core.delegates.getPackageBuilds.GetPackageBuildsDelegate;
import build.archipelago.packageservice.core.delegates.getPackages.GetPackagesDelegate;
import build.archipelago.packageservice.core.delegates.verifyBuildsExists.VerifyBuildsExistsDelegate;
import build.archipelago.packageservice.core.delegates.verifyPackageExists.VerifyPackageExistsDelegate;
import build.archipelago.packageservice.models.ArchipelagoBuiltPackageRestResponse;
import build.archipelago.packageservice.models.CreatePackageRestRequest;
import build.archipelago.packageservice.models.GetPackageBuildRestResponse;
import build.archipelago.packageservice.models.GetPackageBuildsRestResponse;
import build.archipelago.packageservice.models.GetPackageRestResponse;
import build.archipelago.packageservice.models.GetPackagesRestResponse;
import build.archipelago.packageservice.models.VerificationRestRequest;
import build.archipelago.packageservice.models.VerificationRestResponse;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
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
            @RequestBody CreatePackageRestRequest request) throws PackageExistsException {
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


    @GetMapping(value = "all")
    @ResponseStatus(HttpStatus.OK)
    public GetPackagesRestResponse getAllPackages(
            @PathVariable("accountId") String accountId) {
        log.info("Request to get packages for account {}", accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));

        List<GetPackageRestResponse> packageResponse = new ArrayList<>();
        ImmutableList<PackageDetails> packages = getPackagesDelegate.get(accountId);
        for (PackageDetails pkg : packages) {
            packageResponse.add(GetPackageRestResponse.builder()
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
                .packages(packageResponse)
                .build();
    }

    @GetMapping(value = "{name}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("#oauth2.hasScope('http://packageservice.archipelago.build/package.write22')")
    public GetPackageRestResponse getPackage(
            @PathVariable("accountId") String accountId,
            @PathVariable("name") String name
    ) throws PackageNotFoundException {
        log.debug("Request to get information on package {} for account {}", name, accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));

        PackageDetails pkg = getPackageDelegate.get(accountId, name);

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

        return GetPackageBuildsRestResponse.builder()
                .builds(builds.stream()
                        .map(x -> new GetPackageBuildsRestResponse.Build(x.getHash(), x.getCreated().toEpochMilli()))
                        .collect(Collectors.toList()))
                .build();
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

        return GetPackageBuildRestResponse.builder()
                .hash(build.getHash())
                .config(build.getConfig())
                .created(build.getCreated().toEpochMilli())
                .gitCommit(build.getGitCommit())
                .gitBranch(build.getGitBranch())
                .build();
    }

    @GetMapping(value = "{name}/git/{branch}/{commit}")
    @ResponseStatus(HttpStatus.OK)
    public ArchipelagoBuiltPackageRestResponse getPackageByGit(
            @PathVariable("accountId") String accountId,
            @PathVariable("name") String name,
            @PathVariable("branch") String branch,
            @PathVariable("commit") String commit) throws PackageNotFoundException {
        log.info("Request to get package build by git {} (B: {}, C: {}) for account {}", name, branch, commit, accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(branch);
        Preconditions.checkNotNull(commit);

        ArchipelagoBuiltPackage pkg = getPackageBuildByGitDelegate.get(accountId, name, branch, commit);

        return ArchipelagoBuiltPackageRestResponse.builder()
                .name(pkg.getName())
                .version(pkg.getVersion())
                .hash(pkg.getHash())
                .build();
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
