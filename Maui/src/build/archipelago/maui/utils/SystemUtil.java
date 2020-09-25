package build.archipelago.maui.utils;

import java.nio.file.*;

public class SystemUtil {
    public static Path getWorkingPath() {
        return Paths.get(System.getProperty("user.dir"));
    }
    private static Path getHomePath() {
        return Paths.get(System.getProperty("user.home"));
    }
    public static Path getMauiPath() {
        return getHomePath().resolve(".archipelago");
    }
    public static Path getCachePath() {
        return getMauiPath().resolve("cache");
    }
}
