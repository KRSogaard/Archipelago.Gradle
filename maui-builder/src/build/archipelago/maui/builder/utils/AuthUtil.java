package build.archipelago.maui.builder.utils;

import build.archipelago.maui.core.auth.OAuthTokenResponse;
import build.archipelago.maui.core.providers.SystemPathProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class AuthUtil {
    private static ObjectMapper objectMapper = new ObjectMapper();

    public static OAuthTokenResponse getAuthSettings(SystemPathProvider systemPathProvider) {
        Path authFile = systemPathProvider.getMauiPath().resolve(".auth");
        try {
            String authFileContent = Files.readString(authFile);
            return objectMapper.readValue(authFileContent, OAuthTokenResponse.class);
        } catch (Exception exp) {
            return null;
        }
    }
}
