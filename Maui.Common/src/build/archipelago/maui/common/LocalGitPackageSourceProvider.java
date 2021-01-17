package build.archipelago.maui.common;

import build.archipelago.common.github.exceptions.RepoNotFoundException;
import build.archipelago.packageservice.models.PackageDetails;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
public class LocalGitPackageSourceProvider implements PackageSourceProvider {

    public LocalGitPackageSourceProvider() {
    }

    @Override
    public boolean checkOutSource(Path workspaceRoot, PackageDetails packageDetails) throws RepoNotFoundException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(workspaceRoot.toFile());
        processBuilder.command("git", "clone", packageDetails.getGitCloneUrl(), packageDetails.getName());

        try {
            return processBuilder.start().waitFor() == 0;
        } catch (Exception e) {
            log.error("Failed to clone the package from git repository", e);
            return false;
        }
    }

    @Override
    public boolean checkOutSource(Path workspaceRoot, PackageDetails packageDetails, String commit) throws RepoNotFoundException {
        if (!checkOutSource(workspaceRoot, packageDetails)) {
            return false;
        }

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(workspaceRoot.resolve(packageDetails.getName()).toFile());
        processBuilder.command("git", "checkout", commit);

        try {
            return processBuilder.start().waitFor() == 0;
        } catch (Exception e) {
            log.error("Failed to clone the package from git repository", e);
            return false;
        }
    }
}
