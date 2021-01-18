package build.archipelago.harbor.controllers;

import build.archipelago.common.*;
import build.archipelago.common.versionset.*;
import build.archipelago.harbor.filters.AccountIdFilter;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import build.archipelago.versionsetservice.client.models.CreateVersionSetRequest;
import build.archipelago.versionsetservice.exceptions.*;
import build.archipelago.versionsetservice.models.rest.*;
import com.google.common.base.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("version-sets")
@CrossOrigin(origins = "*")
public class VersionSetController {

    private VersionSetServiceClient versionSetServiceClient;

    public VersionSetController(VersionSetServiceClient versionSetServiceClient) {
        Preconditions.checkNotNull(versionSetServiceClient);
        this.versionSetServiceClient = versionSetServiceClient;
    }

    @GetMapping
    public VersionSetsRestResponse getMyVersionSets(@RequestAttribute(AccountIdFilter.Key) String accountId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));

        List<VersionSet> versionSets = versionSetServiceClient.getVersionSets(accountId);
        return VersionSetsRestResponse.builder()
                .versionSets(versionSets.stream().map(VersionSetRestResponse::fromVersionSet).collect(Collectors.toList()))
                .build();

    }


    @GetMapping("{versionSet}")
    public VersionSetRestResponse getVersionSet(@RequestAttribute(AccountIdFilter.Key) String accountId,
                                                @PathVariable("versionSet") String versionSetName) throws VersionSetDoseNotExistsException {
        log.info("Request to get version set '{}'", versionSetName);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName),
                "Version Set name is required");

        VersionSet vs = versionSetServiceClient.getVersionSet(accountId, versionSetName);
        VersionSetRestResponse response = VersionSetRestResponse.fromVersionSet(vs);
        log.debug("Returning version set '{}': {}", versionSetName, response);
        return response;
    }

    @GetMapping("{versionSet}/{revision}")
    public VersionSetRevisionRestResponse getVersionSetRevision(
            @RequestAttribute(AccountIdFilter.Key) String accountId,
            @PathVariable("versionSet") String versionSetName,
            @PathVariable("revision") String revisionId)
            throws VersionSetDoseNotExistsException {
        log.info("Request to get version set packages for '{}' revision '{}'", versionSetName, revisionId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName),
                "Version Set name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(revisionId),
                "Revision id is required");

        VersionSetRevision revision = versionSetServiceClient.getVersionSetPackages(accountId, versionSetName, revisionId);

        VersionSetRevisionRestResponse response = VersionSetRevisionRestResponse.builder()
                .created(revision.getCreated().toEpochMilli())
                .packages(revision.getPackages().stream()
                        .map(ArchipelagoBuiltPackage::toString).collect(Collectors.toList()))
                .build();
        log.debug("Found {} packages for version set \"{}:{}\": {}",
                response.getPackages().size(), versionSetName, revisionId, response);
        return response;
    }

    @PostMapping
    public void createVersionSet(@RequestAttribute(AccountIdFilter.Key) String accountId,
                                 @RequestBody CreateVersionSetRestRequest request)
            throws VersionSetExistsException, VersionSetDoseNotExistsException, PackageNotFoundException {
        log.info("Request to create new version set");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        request.validate();

        Optional<String> parent = Optional.empty();
        if (!Strings.isNullOrEmpty(request.getParent())) {
            parent = Optional.of(request.getParent());
        }
        List<ArchipelagoPackage> targets = new ArrayList<>();
        for (String pkg : request.getTargets()) {
            targets.add(ArchipelagoPackage.parse(pkg));
        }

        CreateVersionSetRequest createRequest = CreateVersionSetRequest.builder()
                .name(request.getName())
                .parent(parent)
                .targets(targets)
                .build();
        versionSetServiceClient.createVersionSet(accountId, createRequest);
    }
}
