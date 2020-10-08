package build.archipelago.maui.core.providers;

import build.archipelago.maui.core.workspace.WorkspaceConstants;
import com.google.common.base.Preconditions;

import java.nio.file.*;

public class SystemPathProvider {

    private Path overrideCurrentDir;

    public Path getCurrentDir() {
        if (overrideCurrentDir != null) {
            return overrideCurrentDir;
        }
        return Paths.get(System.getProperty("user.dir"));
    }

    public void overrideCurrentDir(Path path) {
        Preconditions.checkNotNull(path);

        overrideCurrentDir = path;
    }

    public void removeCurrentDirOverride() {
        overrideCurrentDir = null;
    }


    private Path getHomePath() {
        return Paths.get(System.getProperty("user.home"));
    }

    public Path getMauiPath() {
        return getHomePath().resolve(".archipelago");
    }

    public Path getCachePath() {
        return getMauiPath().resolve("cache");
    }


    public Path getWorkspaceDir() {
        Path currentFolder = getCurrentDir();

        while (currentFolder != null) {
            if (Files.exists(currentFolder.resolve(WorkspaceConstants.WORKSPACE_FILE_NAME))) {
                return currentFolder;
            }
            currentFolder = currentFolder.getParent();
        }
        return null;
    }

    public Path getPackageDir(Path workspaceDir) {
        Preconditions.checkNotNull(workspaceDir);

        Path currentFolder = getCurrentDir();

        while (currentFolder != null) {
            if (currentFolder.equals(workspaceDir)) {
                return null;
            }
            if (Files.exists(currentFolder.resolve(WorkspaceConstants.BUILD_FILE_NAME))) {
                return currentFolder;
            }
            currentFolder = currentFolder.getParent();
        }
        return null;
    }
}
