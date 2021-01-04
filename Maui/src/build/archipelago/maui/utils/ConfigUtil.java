package build.archipelago.maui.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigUtil {
    public static void writeDefaultConfig(Path file) throws IOException {
        String defaultConfig = "" +
                "services.versionset.url: \"http://localhost:8081\"\n" +
                "services.packages.url: \"http://localhost:8080\"\n" +
                "sourceprovider: \"git\"\n" +
                "sourceprovider.git.base: \"https://github.com\"\n" +
                "sourceprovider.git.group: \"KRSogaard\"\n" +
                "oauth.audience: \"http://harbor.archipelago.build\"\n" +
                "oauth.endpoint: \"https://dev-1nl95fdx.us.auth0.com\"\n" +
                "oauth.clientid: \"jg16tUnCZeHx73FbRjm6QcJ9IBzLNm1G\"\n" +
                "services.harbor.url: \"http://localhost:8082\"\n" +
                "sync.threads: 5\n" +
                "";

        Files.writeString(file, defaultConfig);
    }
}
