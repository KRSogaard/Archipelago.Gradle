package build.archipelago.harbor.controllers;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.PackageExistsException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.GetPackageBuildResponse;
import build.archipelago.packageservice.client.models.GetPackageResponse;
import build.archipelago.packageservice.core.delegates.getBuildArtifact.GetBuildArtifactResponse;
import build.archipelago.packageservice.models.CreatePackageRequest;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import build.archipelago.versionsetservice.models.RevisionIdResponse;
import build.archipelago.versionsetservice.models.VersionSetResponse;
import build.archipelago.versionsetservice.models.VersionSetRevisionResponse;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
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
                            Path tempdir) {
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

    @GetMapping(value = {"package/{name}/{version}/{hash}/artifact"})
    public ResponseEntity<Resource> getBuildArtifact(@PathVariable("name") String name,
                                                     @PathVariable("version") String version,
                                                     @PathVariable("hash") String hash) throws PackageNotFoundException, IOException {
        log.info("Request to get build artifact for Package {}, Version: {}, Hash: {}", name, version, hash);
        ArchipelagoPackage pkg = new ArchipelagoPackage(name, version);

        Path file = packageServiceClient.getBuildArtifact(accountId, new ArchipelagoBuiltPackage(name, version, hash), tempdir);

        try {
            String zipFileName = String.format("%s.zip", pkg.toString());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFileName + "\"")
                    .body(new ByteArrayResource(Files.readAllBytes(file)));
        } finally {
            if (Files.exists(file)) {
                Files.delete(file);
            }
        }
    }

    @PostMapping("package/")
    public void createPackage(CreatePackageRequest request) throws PackageExistsException {
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

    @GetMapping("package/{name}/{version}/{hash}/config")
    public String getConfig(@PathVariable("name") String name,
                            @PathVariable("version") String version,
                            @PathVariable("hash") String hash) throws PackageNotFoundException {
        GetPackageBuildResponse response = packageServiceClient.getPackageBuild(
                accountId, new ArchipelagoBuiltPackage(name, version, hash));

        return response.getConfig();
    }

    @GetMapping("package/{name}")
    public GetPackageResponse getPackage(String packageName) throws PackageNotFoundException {
        return packageServiceClient.getPackage(accountId, packageName);
    }

}
