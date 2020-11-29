package build.archipelago.maui.common;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
public class LocalGitPackageSourceProvider implements PackageSourceProvider {

    private String gitBase;
    private String gitGroup;

    public LocalGitPackageSourceProvider(String gitBase,
                                         String gitGroup) {
        this.gitBase = gitBase;
        this.gitGroup = gitGroup;
    }

    @Override
    public boolean checkOutSource(Path workspaceRoot, String packageName) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(workspaceRoot.toFile());
        String gitUrl = getGitClonePath(packageName);
        processBuilder.command("git", "clone", gitUrl, packageName);

        try {
            return processBuilder.start().waitFor() == 0;
        } catch (Exception e) {
            log.error("Failed to clone the package from git repository", e);
            return false;
        }
    }

    private String getGitClonePath(String packageName) {
        StringBuilder sb = new StringBuilder();
        sb.append(gitBase);
        sb.append("/");
        sb.append(gitGroup);
        sb.append("/");
        sb.append(packageName);
        sb.append(".git");
        return sb.toString();
    }

    @Override
    public boolean checkOutSource(Path workspaceRoot, String packageName, String commit) {
        if (!checkOutSource(workspaceRoot, packageName)) {
            return false;
        }

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(workspaceRoot.resolve(packageName).toFile());
        processBuilder.command("git", "checkout", commit);

        try {
            return processBuilder.start().waitFor() == 0;
        } catch (Exception e) {
            log.error("Failed to clone the package from git repository", e);
            return false;
        }
    }
}
