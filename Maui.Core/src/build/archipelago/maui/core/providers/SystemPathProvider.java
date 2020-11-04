package build.archipelago.maui.core.providers;

import build.archipelago.maui.common.WorkspaceConstants;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.*;

@Slf4j
public class SystemPathProvider {

    private Path overrideCurrentDir;

    public Path getCurrentDir() {
        if (overrideCurrentDir != null) {
            log.trace("The current dir was overwritten to \"{}\"", overrideCurrentDir);
            return overrideCurrentDir;
        }
        return Paths.get(System.getProperty("user.dir"));
    }

    public void overrideCurrentDir(Path path) {
        Preconditions.checkNotNull(path);
        log.trace("Overwriting the current dir to \"{}\"", overrideCurrentDir);
        overrideCurrentDir = path;
    }

    public void removeCurrentDirOverride() {
        log.trace("Removing the current dir override");
        overrideCurrentDir = null;
    }

    private Path getHomePath() {
        return Paths.get(System.getProperty("user.home"));
    }

    public Path getMauiPath() {
        return getHomePath().resolve(".archipelago");
    }

    public Path getWorkspaceDir() {
        Path currentFolder = getCurrentDir();
        log.trace("Trying to find the workspace root starting at \"{}\"", currentFolder);

        while (currentFolder != null) {
            if (Files.exists(currentFolder.resolve(WorkspaceConstants.WORKSPACE_FILE_NAME))) {
                log.trace("Workspace was found at \"{}\" as it contained the file {}", currentFolder, WorkspaceConstants.WORKSPACE_FILE_NAME);
                return currentFolder;
            }
            currentFolder = currentFolder.getParent();
        }
        log.debug("Was unable to find the workspace, started at \"{}\"", getCurrentDir());
        return null;
    }

    public Path getPackageDir(Path workspaceDir) {
        Preconditions.checkNotNull(workspaceDir);
        Path currentFolder = getCurrentDir();
        log.trace("Trying to find the package dir starting at \"{}\" with the workspace root being at \"{}\"",
                currentFolder, workspaceDir);

        if (!currentFolder.toString().toLowerCase().startsWith(workspaceDir.toString().toLowerCase())) {
            log.error("The path \"{}\" is not in the workspace \"{}\", the no package dir can be found",
                    currentFolder, workspaceDir);
        }

        while (currentFolder != null) {
            if (currentFolder.equals(workspaceDir)) {
                return null;
            }
            if (Files.exists(currentFolder.resolve(WorkspaceConstants.BUILD_FILE_NAME))) {
                log.trace("Package was found at \"{}\" as it contained the file {}", currentFolder, WorkspaceConstants.BUILD_FILE_NAME);
                return currentFolder;
            }
            currentFolder = currentFolder.getParent();
        }
        log.debug("Was unable to find the package, started at \"{}\"", getCurrentDir());
        return null;
    }
}
