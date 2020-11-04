package build.archipelago.packageservice.controllers;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.packageservice.core.delegates.getBuildArtifact.*;
import build.archipelago.packageservice.core.delegates.uploadBuildArtifact.*;
import build.archipelago.packageservice.models.*;
import com.google.common.base.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping("account/{accountId}/artifact")
@Slf4j
@CrossOrigin(origins = "*")
public class ArtifactController {

    private UploadBuildArtifactDelegate uploadBuildArtifactDelegate;
    private GetBuildArtifactDelegate getBuildArtifactDelegate;

    public ArtifactController(UploadBuildArtifactDelegate uploadBuildArtifactDelegate,
                              GetBuildArtifactDelegate getBuildArtifactDelegate) {
        this.uploadBuildArtifactDelegate = uploadBuildArtifactDelegate;
        this.getBuildArtifactDelegate = getBuildArtifactDelegate;
    }

    @PostMapping("{name}/{version}")
    @ResponseStatus(HttpStatus.OK)
    public ArtifactUploadResponse uploadBuiltArtifact(
            @PathVariable("accountId") String accountId,
            @PathVariable("name") String name,
            @PathVariable("version") String version,
            @ModelAttribute UploadPackageRequest request)
            throws PackageNotFoundException, PackageExistsException {
        log.info("Request to upload new build: {}", request);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "A name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(version), "A version is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getGitCommit()), "A git commit is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getGitBranch()), "A git commit is required");
        Preconditions.checkNotNull(request.getBuildArtifact(),
                "build artifact is required");
        Preconditions.checkArgument(request.getBuildArtifact().getSize() > 0,
                "build artifact is required");
        try {
            String hash = uploadBuildArtifactDelegate.uploadArtifact(
                    UploadBuildArtifactDelegateRequest.builder()
                            .accountId(accountId)
                            .pkg(new ArchipelagoPackage(name, version))
                            .config(request.getConfig())
                            .gitCommit(request.getGitCommit())
                            .gitBranch(request.getGitBranch())
                            .buildArtifact(request.getBuildArtifact().getBytes())
                            .build()
            );
            return ArtifactUploadResponse.builder()
                    .hash(hash)
                    .build();
        } catch (IOException e) {
            log.error("Failed to read build artifact: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping(value = {"{name}/{version}/{hash}", "{name}/{version}"})
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Resource> getBuildArtifact(
            @PathVariable("accountId") String accountId,
            @PathVariable("name") String name,
            @PathVariable("version") String version,
            @PathVariable("hash") Optional<String> hash) throws PackageNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        log.info("Request to get build artifact for Package {}, Version: {}, Hash: {}", name, version, hash);
        ArchipelagoPackage pkg = new ArchipelagoPackage(name, version);

        GetBuildArtifactResponse response = null;
        try {
            response = getBuildArtifactDelegate.getBuildArtifact(accountId, pkg, hash);
        } catch (IOException e) {
            log.error("Unable to read build artifact. " + e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        String zipFileName = String.format("%s.zip", response.getPkg().toString());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFileName + "\"")
                .body(new ByteArrayResource(response.getByteArray()));
    }
}
