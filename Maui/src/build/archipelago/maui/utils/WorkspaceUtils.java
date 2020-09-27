package build.archipelago.maui.utils;

import build.archipelago.maui.core.workspace.WorkspaceConstants;

import java.nio.file.*;

public class WorkspaceUtils {

    public static Path getWorkspaceDir() {
        Path currentFolder = SystemUtil.getWorkingPath();

        while (currentFolder != null) {
            if (Files.exists(currentFolder.resolve(WorkspaceConstants.WORKSPACE_FILE_NAME))) {
                return currentFolder;
            }
            currentFolder = currentFolder.getParent();
        }
        return null;
    }
}
