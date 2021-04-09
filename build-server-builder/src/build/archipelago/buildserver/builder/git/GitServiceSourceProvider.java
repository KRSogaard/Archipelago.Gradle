package build.archipelago.buildserver.builder.git;

import build.archipelago.common.github.GitService;
import build.archipelago.common.git.models.exceptions.RepoNotFoundException;
import build.archipelago.maui.common.PackageSourceProvider;
import build.archipelago.packageservice.models.PackageDetails;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GitServiceSourceProvider implements PackageSourceProvider {

    private GitService gitService;

    public GitServiceSourceProvider(GitService gitService) {
        this.gitService = gitService;
    }

    @Override
    public boolean checkOutSource(Path workspaceRoot, PackageDetails packageDetails) throws RepoNotFoundException {
        return checkOutSource(workspaceRoot, packageDetails, "master");
    }

    @Override
    public boolean checkOutSource(Path workspaceRoot, PackageDetails packageDetails, String commit) throws RepoNotFoundException {
        log.info("Fetching git source code for package '{}' commit '{}", packageDetails.getGitRepoFullName(), commit);
        Path filePath = workspaceRoot.resolve(packageDetails.getName() + "-" + commit + ".zip");
        gitService.downloadRepoZip(filePath, packageDetails.getGitRepoFullName(), commit);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Downloading GIT repo " + packageDetails.getGitRepoFullName() +
                    " failed with unknown reason");
        }

        try {
            Path packagePath = workspaceRoot.resolve(packageDetails.getName());
            ZipFile zip = new ZipFile(filePath.toFile());
            log.debug("Unzipping '{}' to '{}'", filePath, packagePath);
            zip.extractAll(packagePath.toAbsolutePath().toString());
            // We need to move all files up one dir
            try {
                List<Path> files = Files.list(packagePath).collect(Collectors.toList());
                log.debug("Found {} files in '{}'", files.size(), packagePath);
                if (files.size() != 1) {
                    log.warn("The github zip was not correctly formatted, the file count was {}", files.size());
                    throw new RuntimeException("Github zip was not correctly formatted");
                }

                if (Files.exists(files.get(0)) && Files.isDirectory(files.get(0))) {
                    for (Path file : Files.list(files.get(0)).collect(Collectors.toList())) {
                        Files.move(file, packagePath.resolve(file.getFileName()));
                    }
                    Files.delete(files.get(0));
                } else  {
                    throw new RuntimeException("Github zip was not correctly formatted");
                }
            } catch (IOException e) {
                log.error("IOException while unzipping source code", e);
                e.printStackTrace();
            }
        } catch (ZipException e) {
            log.error("Exception while unzipping source code", e);
            throw new RuntimeException(e);
        } finally {
            if (Files.exists(filePath)) {
                try {
                    Files.delete(filePath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }
}
