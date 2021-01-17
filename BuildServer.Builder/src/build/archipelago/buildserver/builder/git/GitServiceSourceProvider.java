package build.archipelago.buildserver.builder.git;

import build.archipelago.common.github.GitService;
import build.archipelago.common.github.exceptions.RepoNotFoundException;
import build.archipelago.maui.common.PackageSourceProvider;
import build.archipelago.packageservice.models.PackageDetails;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

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
        Path filePath = workspaceRoot.resolve(packageDetails.getName() + "-" + commit + ".zip");
        gitService.downloadRepoZip(filePath, packageDetails.getGitRepoFullName(), commit);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Downloading GIT repo " + packageDetails.getGitRepoFullName() +
                    " failed with unknown reason");
        }

        try {
            Path packagePath = workspaceRoot.resolve(packageDetails.getName());
            ZipFile zip = new ZipFile(filePath.toFile());
            zip.extractAll(packagePath.toAbsolutePath().toString());
            // We need to move all files up one dir
            try {
                List<Path> files = Files.list(packagePath).collect(Collectors.toList());
                if (files.size() != 1) {
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
                e.printStackTrace();
            }
        } catch (ZipException e) {
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
