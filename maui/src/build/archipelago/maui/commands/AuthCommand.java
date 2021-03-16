package build.archipelago.maui.commands;

import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.actions.BaseAction;
import build.archipelago.maui.core.auth.AuthService;
import build.archipelago.maui.core.auth.OAuthDeviceCodeResponse;
import build.archipelago.maui.core.auth.OAuthTokenResponse;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.utils.AuthUtil;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Slf4j
@CommandLine.Command(name = "auth", mixinStandardHelpOptions = true, description = "Authenticate")
public class AuthCommand extends BaseAction implements Callable<Integer> {

    private com.fasterxml.jackson.databind.ObjectMapper objectMapper
            = new com.fasterxml.jackson.databind.ObjectMapper();

    private AuthService authService;

    public AuthCommand(WorkspaceContextFactory workspaceContextFactory,
                       SystemPathProvider systemPathProvider,
                       OutputWrapper out,
                       AuthService authService) {
        super(workspaceContextFactory, systemPathProvider, out);
        this.authService = authService;
    }

    @Override
    public Integer call() {
        Path authFile = systemPathProvider.getMauiPath().resolve(".auth");

        OAuthDeviceCodeResponse deviceCodeResponse = authService.getDeviceCode();
        if (deviceCodeResponse == null) {
            out.error("Failed to request an authentication token, please check you internet connection.");
            return 1;
        }

        out.write("\n");
        out.write("Please login at " + deviceCodeResponse.getVerificationUri());
        out.write("Your code is: " + deviceCodeResponse.getUserCode());
        out.write("Or use this link: " + deviceCodeResponse.getVerificationUriComplete());

        OAuthTokenResponse tokenResponse = authService.getToken(deviceCodeResponse);
        if (tokenResponse == null) {
            out.error("Failed to authentication");
            return 1;
        }

        try {
            AuthUtil.saveAuthSettings(systemPathProvider, tokenResponse);
        } catch (RuntimeException exp) {
            out.error("Failed to store the auth settings");
            return 1;
        }

        out.write("Authentication complete, your login expires in %d hours",tokenResponse.getExpiresIn() / (60 * 60));
        return 0;
    }
}
