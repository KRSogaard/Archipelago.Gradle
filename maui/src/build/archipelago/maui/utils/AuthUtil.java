package build.archipelago.maui.utils;

import build.archipelago.maui.clients.UnauthorizedHarborClient;
import build.archipelago.maui.core.auth.OAuthTokenResponse;
import build.archipelago.maui.core.providers.SystemPathProvider;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class AuthUtil {
    private static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static OAuthTokenResponse getAuthSettings(SystemPathProvider systemPathProvider) {
        Path authFile = systemPathProvider.getMauiPath().resolve(".auth");
        try {
            String authFileContent = Files.readString(authFile);
            return objectMapper.readValue(authFileContent, OAuthTokenResponse.class);
        } catch (Exception exp) {
            return null;
        }
    }

    public static void saveAuthSettings(SystemPathProvider systemPathProvider, OAuthTokenResponse oauth) {
        Path authFile = systemPathProvider.getMauiPath().resolve(".auth");
        try {
            Files.writeString(authFile, objectMapper.writeValueAsString(oauth));
        } catch (IOException e) {
            log.error("Failed to write authentication token to disk", e);
            throw new RuntimeException("Failed to store the auth settings");
        }
    }
}
