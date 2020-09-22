package build.archipelago.maui.utils;

import java.nio.file.*;

public class SystemUtil {
    public static Path getWorkingPath() {
        return Paths.get(System.getProperty("user.dir"));
    }
}
