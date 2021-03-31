package build.archipelago.maui.utils;

import java.io.IOException;
import java.nio.file.*;

public class ConfigUtil {
    public static void writeDefaultConfig(Path file) throws IOException {
        String defaultConfig = "" +
                "oauth.endpoint: http://auth.alpha.archipelago.build\n" +
                "oauth.clientid: IGrxIc1VpifO5qZPvkWN\n" +
                "services.harbor.url: http://harbor.alpha.archipelago.build\n" +
                "sync.threads: 5\n" +
                "";

        Files.writeString(file, defaultConfig);
    }
}
