package build.archipelago.versionsetservice.controllers;

import build.archipelago.common.*;
import build.archipelago.common.versionset.*;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.versionsetservice.core.delegates.*;
import build.archipelago.versionsetservice.core.delegates.addCallback.AddCallbackDelegate;
import build.archipelago.versionsetservice.core.delegates.createVersionSet.CreateVersionSetDelegate;
import build.archipelago.versionsetservice.core.delegates.createVersionSet.CreateVersionSetRequest;
import build.archipelago.versionsetservice.core.delegates.deleteCallback.DeleteCallbackDelegate;
import build.archipelago.versionsetservice.core.delegates.getCallbacks.GetCallbacksDelegate;
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
    private UpdateVersionSetDelegate updateVersionSetDelegate;
    private GetCallbacksDelegate getCallbacksDelegate;
    private DeleteCallbackDelegate deleteCallbackDelegate;
    private AddCallbackDelegate addCallbackDelegate;

    public VersionSetController(CreateVersionSetDelegate createVersionSetDelegate,
                                CreateVersionSetRevisionDelegate createVersionSetRevisionDelegate,
                                GetVersionSetDelegate getVersionSetDelegate,
                                GetVersionSetPackagesDelegate getVersionSetPackagesDelegate,
                                GetVersionSetsDelegate getVersionSetsDelegate,
                                UpdateVersionSetDelegate updateVersionSetDelegate,
                                GetCallbacksDelegate getCallbacksDelegate,
                                DeleteCallbackDelegate deleteCallbackDelegate,
                                AddCallbackDelegate addCallbackDelegate) {
        this.createVersionSetDelegate = createVersionSetDelegate;
        this.createVersionSetRevisionDelegate = createVersionSetRevisionDelegate;
        this.getVersionSetDelegate = getVersionSetDelegate;
        this.getVersionSetPackagesDelegate = getVersionSetPackagesDelegate;
        this.getVersionSetsDelegate = getVersionSetsDelegate;
        this.updateVersionSetDelegate = updateVersionSetDelegate;
        this.getCallbacksDelegate = getCallbacksDelegate;
        this.deleteCallbackDelegate = deleteCallbackDelegate;
        this.addCallbackDelegate = addCallbackDelegate;
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


        ArchipelagoPackage target = null;
        if (request.getTarget() != null) {
            target = ArchipelagoPackage.parse(request.getTarget());
        }

        String parent = null;
        if (!Strings.isNullOrEmpty(request.getParent())) {
            parent = request.getParent();
        }
        createVersionSetDelegate.create(CreateVersionSetRequest.builder()
                .accountId(accountId)
                .name(request.getName())
                .target(target)
                .parent(parent)
                .build());
        log.debug("Version set '{}' was successfully installed", request.getName());
    }

    @PostMapping("/{versionSet}")
    @ResponseStatus(HttpStatus.OK)
    public CreateVersionSetRevisionRestResponse createVersionSetRevision(
            @PathVariable("accountId") String accountId,
            @PathVariable("versionSet") String versionSetName,
            @RequestBody CreateVersionSetRevisionRestRequest request) throws VersionSetDoseNotExistsException,
            MissingTargetPackageException, PackageNotFoundException {
        log.info("Request to created revision for version set '{}' for account '{}'", versionSetName, accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName));
        request.validate();

        ArchipelagoPackage target = null;
        if (!Strings.isNullOrEmpty(request.getTarget())) {
            target = ArchipelagoPackage.parse(request.getTarget());
        }

        List<ArchipelagoBuiltPackage> packages = request.getPackages().stream()
                .map(ArchipelagoBuiltPackage::parse).collect(Collectors.toList());

        String revisionId = createVersionSetRevisionDelegate.createRevision(
                accountId, versionSetName, packages, target);

        log.debug("New revision '{}' was created for version set '{}'", revisionId, versionSetName);
        return CreateVersionSetRevisionRestResponse.builder()
                .revisionId(revisionId)
                .build();
    }

    @PutMapping("/{versionSet}")
    @ResponseStatus(HttpStatus.OK)
    public void updateVersionSet(
            @PathVariable("accountId") String accountId,
            @PathVariable("versionSet") String versionSetName,
            @RequestBody UpdateVersionSetRestRequest request) throws PackageNotFoundException, VersionSetDoseNotExistsException {
        log.info("Request to update revision for version set '{}' for account '{}'", versionSetName, accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName));
        request.validate();


        updateVersionSetDelegate.update(accountId, versionSetName, request.toInternal());
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
                .target(revision.getTarget() == null ? null : revision.getTarget().getNameVersion())
                .packages(revision.getPackages().stream()
                        .map(ArchipelagoBuiltPackage::toString).collect(Collectors.toList()))
                .build();
        log.debug("Found {} packages for version set \"{}:{}\": {}",
                response.getPackages().size(), versionSetName, revisionId, response);
        return response;
    }

    @GetMapping("{versionSet}/callbacks")
    @ResponseStatus(HttpStatus.OK)
    public VersionSetCallbacksRestResponse getGetCallbacks(
            @PathVariable("accountId") String accountId,
            @PathVariable("versionSet") String versionSetName) throws VersionSetDoseNotExistsException {
        log.info("Request to get version set callbacks for '{}'", versionSetName);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName),
                "Version Set name is required");

        List<VersionSetCallback> callbacks = getCallbacksDelegate.getCallbacks(accountId, versionSetName);
        return VersionSetCallbacksRestResponse.from(callbacks);
    }

    @DeleteMapping("{versionSet}/callbacks/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void removeCallbacks(
            @PathVariable("accountId") String accountId,
            @PathVariable("versionSet") String versionSetName,
            @PathVariable("id") String id) throws VersionSetDoseNotExistsException {
        log.info("Request to get version set callbacks for '{}'", versionSetName);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName),
                "Version Set name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(id),
                "Callback id is required");

        deleteCallbackDelegate.deleteCallback(accountId, versionSetName, id);
    }

    @PostMapping("{versionSet}/callbacks")
    @ResponseStatus(HttpStatus.OK)
    public void addCallbacks(
            @PathVariable("accountId") String accountId,
            @PathVariable("versionSet") String versionSetName,
            @RequestBody AddCallbackRestRequest request) throws VersionSetDoseNotExistsException {
        log.info("Request to get version set callbacks for '{}'", versionSetName);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName),
                "Version Set name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getUrl()),
                "Callback id is required");

        addCallbackDelegate.addCallback(accountId, versionSetName, request.getUrl());
    }


}
