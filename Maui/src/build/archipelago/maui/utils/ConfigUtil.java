package build.archipelago.maui.utils;

import java.io.IOException;
import java.nio.file.*;

public class ConfigUtil {
    public static void writeDefaultConfig(Path file) throws IOException {
        String defaultConfig = "" +
                "services.versionset.url: \"http://localhost:8081\"\n" +
                "services.packages.url: \"http://localhost:8080\"\n";

        Files.writeString(file, defaultConfig);
    }
}
