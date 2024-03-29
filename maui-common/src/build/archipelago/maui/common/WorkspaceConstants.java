package build.archipelago.maui.common;

import java.util.List;

public class WorkspaceConstants {
    public static final String VERSION_SET_REVISION_CACHE = "revision.cache";
    public static final String TEMP_FOLDER = ".archipelago";
    public static final String WORKSPACE_FILE_NAME = "ARCHIPELAGO";
    public static final String BUILD_FILE_NAME = "ISLAND";
    public static final String BUILD_DIR = "build";

    public static final List<String> IGNORE_BUILD_FOLDERS = List.of(
            "private"
    );
}
