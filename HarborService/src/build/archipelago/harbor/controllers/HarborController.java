package build.archipelago.harbor.controllers;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import build.archipelago.versionsetservice.models.RevisionIdResponse;
import build.archipelago.versionsetservice.models.VersionSetResponse;
import build.archipelago.versionsetservice.models.VersionSetRevisionResponse;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.stream.Collectors;

@RestController
@RequestMapping("")
@Slf4j
@CrossOrigin(origins = "*")
public class HarborController {

    private String accountId = "wewelo";
    private Path tempdir;
    private VersionSetServiceClient versionSetServiceClient;
    private PackageServiceClient packageServiceClient;

    public HarborController(VersionSetServiceClient versionSetServiceClient,
                            PackageServiceClient packageServiceClient,
                            @Qualifier("tempDir") Path tempdir) {
        Preconditions.checkNotNull(versionSetServiceClient);
        this.versionSetServiceClient = versionSetServiceClient;
        Preconditions.checkNotNull(packageServiceClient);
        this.packageServiceClient = packageServiceClient;
        this.tempdir = tempdir;
    }

    @GetMapping("version-set/{versionSet}")
    public VersionSetResponse getVersionSet(@PathVariable("versionSet") String versionSetName) throws VersionSetDoseNotExistsException {
        log.info("Request to get version set \"{}\"", versionSetName);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName),
                "Version Set name is required");

        VersionSet vs = versionSetServiceClient.getVersionSet(accountId, versionSetName);

        VersionSetResponse response = VersionSetResponse.builder()
                .name(vs.getName())
                .created(vs.getCreated().toEpochMilli())
                .parent(vs.getParent())
                .targets(vs.getTargets().stream().map(ArchipelagoPackage::toString).collect(Collectors.toList()))
                .revisions(vs.getRevisions().stream().map(RevisionIdResponse::from).collect(Collectors.toList()))
                .latestRevision(vs.getLatestRevision())
                .latestRevisionCreated(
                        vs.getLatestRevisionCreated() != null ? vs.getLatestRevisionCreated().toEpochMilli() : null)
                .build();
        log.debug("Returning version set \"{}\": {}", versionSetName, response);
        return response;
    }

    @GetMapping("version-set/{versionSet}/{revision}")
    public VersionSetRevisionResponse getVersionSetRevision(
            @PathVariable("versionSet") String versionSetName,
            @PathVariable("revision") String revisionId)
            throws VersionSetDoseNotExistsException {
        log.info("Request to get version set packages for \"{}\" revision \"{}\"", versionSetName, revisionId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName),
                "Version Set name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(revisionId),
                "Revision id is required");

        VersionSetRevision revision = versionSetServiceClient.getVersionSetPackages(accountId, versionSetName, revisionId);

        VersionSetRevisionResponse response = VersionSetRevisionResponse.builder()
                .created(revision.getCreated().toEpochMilli())
                .packages(revision.getPackages().stream()
                        .map(ArchipelagoBuiltPackage::toString).collect(Collectors.toList()))
                .build();
        log.debug("Found {} packages for version set \"{}:{}\": {}",
                response.getPackages().size(), versionSetName, revisionId, response);
        return response;
    }



}
