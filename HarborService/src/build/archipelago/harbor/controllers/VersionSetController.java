package build.archipelago.harbor.controllers;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.exceptions.VersionSetExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.harbor.filters.AccountIdFilter;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import build.archipelago.versionsetservice.client.models.CreateVersionSetRequest;
import build.archipelago.versionsetservice.models.CreateVersionSetRestRequest;
import build.archipelago.versionsetservice.models.VersionSetRestResponse;
import build.archipelago.versionsetservice.models.VersionSetRevisionRestResponse;
import build.archipelago.versionsetservice.models.VersionSetsRestResponse;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        log.info("Request to get version set \"{}\"", versionSetName);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName),
                "Version Set name is required");

        VersionSet vs = versionSetServiceClient.getVersionSet(accountId, versionSetName);
        VersionSetRestResponse response = VersionSetRestResponse.fromVersionSet(vs);
        log.debug("Returning version set \"{}\": {}", versionSetName, response);
        return response;
    }

    @GetMapping("{versionSet}/{revision}")
    public VersionSetRevisionRestResponse getVersionSetRevision(
            @RequestAttribute(AccountIdFilter.Key) String accountId,
            @PathVariable("versionSet") String versionSetName,
            @PathVariable("revision") String revisionId)
            throws VersionSetDoseNotExistsException {
        log.info("Request to get version set packages for \"{}\" revision \"{}\"", versionSetName, revisionId);
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
