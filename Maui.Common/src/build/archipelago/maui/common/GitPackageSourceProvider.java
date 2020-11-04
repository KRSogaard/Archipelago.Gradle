package build.archipelago.maui.common;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
public class GitPackageSourceProvider implements PackageSourceProvider {

    private String gitBase;
    private String gitGroup;

    public GitPackageSourceProvider(String gitBase,
                                    String gitGroup) {
        this.gitBase = gitBase;
        this.gitGroup = gitGroup;
    }

    @Override
    public boolean checkOutSource(String packageName, Path workspaceRoot) {
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
}