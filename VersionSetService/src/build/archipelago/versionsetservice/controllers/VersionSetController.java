package build.archipelago.versionsetservice.controllers;

import build.archipelago.common.*;
import build.archipelago.common.versionset.*;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.versionsetservice.core.delegates.*;
import build.archipelago.versionsetservice.exceptions.*;
import build.archipelago.versionsetservice.models.rest.*;
import com.google.common.base.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/account/{accountId}/version-set")
@Slf4j
public class VersionSetController {

    private CreateVersionSetDelegate createVersionSetDelegate;
    private CreateVersionSetRevisionDelegate createVersionSetRevisionDelegate;
    private GetVersionSetDelegate getVersionSetDelegate;
    private GetVersionSetPackagesDelegate getVersionSetPackagesDelegate;
    private GetVersionSetsDelegate getVersionSetsDelegate;

    public VersionSetController(CreateVersionSetDelegate createVersionSetDelegate,
                                CreateVersionSetRevisionDelegate createVersionSetRevisionDelegate,
                                GetVersionSetDelegate getVersionSetDelegate,
                                GetVersionSetPackagesDelegate getVersionSetPackagesDelegate,
                                GetVersionSetsDelegate getVersionSetsDelegate) {
        this.createVersionSetDelegate = createVersionSetDelegate;
        this.createVersionSetRevisionDelegate = createVersionSetRevisionDelegate;
        this.getVersionSetDelegate = getVersionSetDelegate;
        this.getVersionSetPackagesDelegate = getVersionSetPackagesDelegate;
        this.getVersionSetsDelegate = getVersionSetsDelegate;
    }

    @GetMapping
    public VersionSetsRestResponse getVersionSets(@PathVariable("accountId") String accountId) {
        log.info("Request to get all version sets for {}", accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");

        List<VersionSet> versionSets = getVersionSetsDelegate.getVersionSets(accountId);
        return VersionSetsRestResponse.builder()
                .versionSets(versionSets.stream().map(VersionSetRestResponse::fromVersionSet).collect(Collectors.toList()))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void createVersionSet(
            @PathVariable("accountId") String accountId,
            @RequestBody CreateVersionSetRestRequest request) throws
            VersionSetDoseNotExistsException, VersionSetExistsException, PackageNotFoundException {
        log.info("Create version Set for account {}, Request: {}", accountId, request);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        request.validate();

        List<ArchipelagoPackage> targets = request.getTargets().stream()
                .map(ArchipelagoPackage::parse).collect(Collectors.toList());

        Optional<String> parent = Optional.empty();
        if (!Strings.isNullOrEmpty(request.getParent())) {
            parent = Optional.of(request.getParent());
        }
        createVersionSetDelegate.create(accountId, request.getName(), targets, parent);
        log.debug("Version set '{}' was successfully installed", request.getName());
    }

    @PostMapping("/{versionSet}")
    @ResponseStatus(HttpStatus.OK)
    public CreateVersionSetRevisionRestResponse createVersionSetRevision(
            @PathVariable("accountId") String accountId,
            @PathVariable("versionSet") String versionSetName,
            @RequestBody CreateVersionSetRevisionRestRequest request) throws VersionSetDoseNotExistsException,
            MissingTargetPackageException, PackageNotFoundException {
        log.info("Request to created revision for version set '{}': {}", versionSetName, request);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName));
        request.validate();

        List<ArchipelagoBuiltPackage> packages = request.getPackages().stream()
                .map(ArchipelagoBuiltPackage::parse).collect(Collectors.toList());

        String revisionId = createVersionSetRevisionDelegate.createRevision(
                accountId, versionSetName, packages);

        log.debug("New revision '{}' was created for version set '{}'", revisionId, versionSetName);
        return CreateVersionSetRevisionRestResponse.builder()
                .revisionId(revisionId)
                .build();
    }

    @GetMapping("{versionSet}")
    @ResponseStatus(HttpStatus.OK)
    public VersionSetRestResponse getVersionSet(
            @PathVariable("accountId") String accountId,
            @PathVariable("versionSet") String versionSetName)
            throws VersionSetDoseNotExistsException {
        log.info("Request to get version set '{}'", versionSetName);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName),
                "Version Set name is required");

        VersionSet vs = getVersionSetDelegate.getVersionSet(accountId, versionSetName);

        VersionSetRestResponse response = VersionSetRestResponse.fromVersionSet(vs);
        log.debug("Returning version set '{}': {}", versionSetName, response);
        return response;
    }

    @GetMapping("{versionSet}/{revision}")
    @ResponseStatus(HttpStatus.OK)
    public VersionSetRevisionRestResponse getVersionSetPackages(
            @PathVariable("accountId") String accountId,
            @PathVariable("versionSet") String versionSetName,
            @PathVariable("revision") String revisionId)
            throws VersionSetDoseNotExistsException {
        log.info("Request to get version set packages for '{}' revision '{}'", versionSetName, revisionId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName),
                "Version Set name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(revisionId),
                "Revision id is required");

        VersionSetRevision revision = getVersionSetPackagesDelegate.getPackages(accountId, versionSetName, revisionId);

        VersionSetRevisionRestResponse response = VersionSetRevisionRestResponse.builder()
                .created(revision.getCreated().toEpochMilli())
                .packages(revision.getPackages().stream()
                        .map(ArchipelagoBuiltPackage::toString).collect(Collectors.toList()))
                .build();
        log.debug("Found {} packages for version set \"{}:{}\": {}",
                response.getPackages().size(), versionSetName, revisionId, response);
        return response;
    }

}
