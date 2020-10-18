package build.archipelago.packageservice.controllers;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.packageservice.core.data.models.*;
import build.archipelago.packageservice.core.delegates.createPackage.*;
import build.archipelago.packageservice.core.delegates.getPackage.GetPackageDelegate;
import build.archipelago.packageservice.core.delegates.getPackageBuild.GetPackageBuildDelegate;
import build.archipelago.packageservice.core.delegates.getPackageBuildByGit.GetPackageBuildByGitDelegate;
import build.archipelago.packageservice.core.delegates.getPackageBuilds.GetPackageBuildsDelegate;
import build.archipelago.packageservice.core.delegates.verifyBuildsExists.VerifyBuildsExistsDelegate;
import build.archipelago.packageservice.core.delegates.verifyPackageExists.VerifyPackageExistsDelegate;
import build.archipelago.packageservice.models.*;
import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping
@Slf4j
public class PackagesController {

    private CreatePackageDelegate createPackageDelegate;
    private GetPackageDelegate getPackageDelegate;
    private GetPackageBuildsDelegate getPackageBuildsDelegate;
    private GetPackageBuildDelegate getPackageBuildDelegate;
    private GetPackageBuildByGitDelegate getPackageBuildByGitDelegate;
    private VerifyBuildsExistsDelegate verifyBuildsExistsDelegate;
    private VerifyPackageExistsDelegate verifyPackageExistsDelegate;

    public PackagesController(GetPackageDelegate getPackageDelegate,
                              CreatePackageDelegate createPackageDelegate,
                              GetPackageBuildsDelegate getPackageBuildsDelegate,
                              GetPackageBuildDelegate getPackageBuildDelegate,
                              GetPackageBuildByGitDelegate getPackageBuildByGitDelegate,
                              VerifyBuildsExistsDelegate verifyBuildsExistsDelegate,
                              VerifyPackageExistsDelegate verifyPackageExistsDelegate) {
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
    }

    @PostMapping(value = "{accountId}/package")
    @ResponseStatus(HttpStatus.OK)
    public void createPackage(
            @PathVariable("accountId") String accountId,
            @RequestBody CreatePackageRequest request) throws PackageExistsException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(request);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getName()));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getDescription()));

        createPackageDelegate.create(CreatePackageDelegateRequest.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build());
    }

    @GetMapping(value = "{accountId}/package/{name}")
    @ResponseStatus(HttpStatus.OK)
    public GetPackageResponse getPackage(
            @PathVariable("accountId") String accountId,
            @PathVariable("name") String name
    ) throws PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));

        PackageDetails pkg = getPackageDelegate.get(accountId, name);

        return GetPackageResponse.builder()
                .name(pkg.getName())
                .description(pkg.getDescription())
                .created(pkg.getCreated().toEpochMilli())
                .versions(pkg.getVersions().stream().map(x -> new GetPackageResponse.Version(
                        x.getVersion(),
                        x.getLatestBuildHash(),
                        x.getLatestBuildTime().toEpochMilli())).collect(Collectors.toList()))
                .build();
    }

    @GetMapping(value = "{accountId}/package/{name}/{version}")
    @ResponseStatus(HttpStatus.OK)
    public GetPackageBuildsResponse getPackageBuilds(
            @PathVariable("accountId") String accountId,
            @PathVariable("name") String name,
            @PathVariable("version") String version
    ) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(version));
        ArchipelagoPackage pkg = new ArchipelagoPackage(name, version);
        log.info("Request to get builds for {}", pkg);

        ImmutableList<VersionBuildDetails> builds = getPackageBuildsDelegate.get(accountId, pkg);

        return GetPackageBuildsResponse.builder()
                .builds(builds.stream()
                        .map(x -> new GetPackageBuildsResponse.Build(x.getHash(), x.getCreated().toEpochMilli()))
                        .collect(Collectors.toList()))
                .build();
    }

    @GetMapping(value = "{accountId}/package/{name}/{version}/{hash}")
    @ResponseStatus(HttpStatus.OK)
    public GetPackageBuildResponse getPackageBuild(
            @PathVariable("accountId") String accountId,
            @PathVariable("name") String name,
            @PathVariable("version") String version,
            @PathVariable("hash") String hash
    ) throws PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(version));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(hash));
        ArchipelagoBuiltPackage pkg = new ArchipelagoBuiltPackage(name, version, hash);
        log.info("Request to get build details for {}", pkg);

        BuiltPackageDetails build = getPackageBuildDelegate.get(accountId, pkg);

        return GetPackageBuildResponse.builder()
                .hash(build.getHash())
                .config(build.getConfig())
                .created(build.getCreated().toEpochMilli())
                .gitCommit(build.getGitCommit())
                .gitBranch(build.getGitBranch())
                .build();
    }

    @GetMapping(value = "{accountId}/package/{name}/git/{branch}/{commit}")
    @ResponseStatus(HttpStatus.OK)
    public ArchipelagoBuiltPackageResponse getPackageByGit(
            @PathVariable("accountId") String accountId,
            @PathVariable("name") String name,
            @PathVariable("branch") String branch,
            @PathVariable("commit") String commit) throws PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(branch);
        Preconditions.checkNotNull(commit);

        ArchipelagoBuiltPackage pkg = getPackageBuildByGitDelegate.get(accountId, name, branch, commit);

        return ArchipelagoBuiltPackageResponse.builder()
                .name(pkg.getName())
                .version(pkg.getVersion())
                .hash(pkg.getHash())
                .build();
    }

    @PostMapping(value = "{accountId}/package/verify-packages")
    @ResponseStatus(HttpStatus.OK)
    public VerificationResponse verifyPackages(
            @PathVariable("accountId") String accountId,
            @RequestBody VerificationRequest request) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        ImmutableList.Builder<ArchipelagoPackage> packages = ImmutableList.builder();
        for (String pkg : request.getPackages()) {
            packages.add(ArchipelagoPackage.parse(pkg));
        }
        ImmutableList<ArchipelagoPackage> pkgs = packages.build();
        log.info("Verifying packages for: {}", pkgs);

        ImmutableList<ArchipelagoPackage> missing = verifyPackageExistsDelegate.verify(accountId, pkgs);
        return VerificationResponse.builder()
                .missing(missing.stream().map(ArchipelagoPackage::getNameVersion).collect(Collectors.toList()))
                .build();
    }

    @PostMapping(value = "{accountId}/package/verify-builds")
    @ResponseStatus(HttpStatus.OK)
    public VerificationResponse verifyBuilds(
            @PathVariable("accountId") String accountId,
            @RequestBody VerificationRequest request) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        ImmutableList.Builder<ArchipelagoBuiltPackage> packages = ImmutableList.builder();
        for (String pkg : request.getPackages()) {
            packages.add(ArchipelagoBuiltPackage.parse(pkg));
        }
        ImmutableList<ArchipelagoBuiltPackage> pkgs = packages.build();
        log.info("Verifying builds for: {}", pkgs);

        ImmutableList<ArchipelagoBuiltPackage> missing = verifyBuildsExistsDelegate.verify(accountId, pkgs);
        return VerificationResponse.builder()
                .missing(missing.stream().map(ArchipelagoBuiltPackage::toString).collect(Collectors.toList()))
                .build();
    }
}
