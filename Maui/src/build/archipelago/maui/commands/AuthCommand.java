package build.archipelago.maui.commands;

import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.actions.BaseAction;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.models.OAuthDeviceCodeResponse;
import build.archipelago.maui.models.OAuthTokenResponse;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Slf4j
@CommandLine.Command(name = "auth", mixinStandardHelpOptions = true, description = "Authenticate")
public class AuthCommand extends BaseAction implements Callable<Integer> {

    private com.fasterxml.jackson.databind.ObjectMapper objectMapper
            = new com.fasterxml.jackson.databind.ObjectMapper();
    private static String audience = "http://harbor.archipelago.build";
    private String authEndpoint;
    private String clientId;

    public AuthCommand(WorkspaceContextFactory workspaceContextFactory,
                       SystemPathProvider systemPathProvider,
                       OutputWrapper out,
                       String authEndpoint,
                       String clientId) {
        super(workspaceContextFactory, systemPathProvider, out);
        this.authEndpoint = authEndpoint;
        this.clientId = clientId;
    }

    @Override
    public Integer call() {
        Path authFile = systemPathProvider.getMauiPath().resolve(".auth");
        HttpClient client = HttpClient.newHttpClient();

        OAuthDeviceCodeResponse deviceCodeResponse = getDeviceCode();
        if (deviceCodeResponse == null) {
            out.error("Failed to request an authentication token");
            return 1;
        }

        out.write("\n");
        out.write("Please login at " + deviceCodeResponse.getVerificationUri());
        out.write("Your code is: " + deviceCodeResponse.getUserCode());
        out.write("Or use this link: " + deviceCodeResponse.getVerificationUriComplete());

        OAuthTokenResponse tokenResponse = getToken(deviceCodeResponse);
        if (deviceCodeResponse == null) {
            out.error("Failed to authentication");
            return 1;
        }

        try {
            Files.writeString(authFile, objectMapper.writeValueAsString(tokenResponse));
        } catch (IOException e) {
            log.error("Failed to write authentication token to disk", e);
            out.error("Failed to write authentication token to disk");
            return 1;
        }

        out.write("Authentication complete, your login expires in %d hours",tokenResponse.getExpiresIn() / (60 * 60));
        return 0;
    }

    private OAuthDeviceCodeResponse getDeviceCode() {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", clientId);
        parameters.put("audience", audience);

        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(new URI(authEndpoint + "/oauth/device/code"))
                    .header("content-type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(mapToFormString(parameters)))
                    .build();
            httpResponse = HttpClient.newHttpClient()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (200 != httpResponse.statusCode()) {
                log.error("Failed to request an authentication token, got http status code {}", httpResponse.statusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to request an authentication token", e);
            return null;
        }

        OAuthDeviceCodeResponse response;
        try {

            response = objectMapper.readValue(httpResponse.body(), OAuthDeviceCodeResponse.class);
        } catch (IOException e) {
            log.error(String.format("The device key response was not the expected format \"%s\"", httpResponse.body()), e);
            return null;
        }
        return response;
    }

    private OAuthTokenResponse getToken(OAuthDeviceCodeResponse oAuthDeviceCodeResponse) {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", clientId);
        parameters.put("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
        parameters.put("device_code", oAuthDeviceCodeResponse.getDeviceCode());

        Instant expires = Instant.now().plusSeconds(oAuthDeviceCodeResponse.getExpiresIn());
        HttpResponse<String> httpResponse = null;
        while ((httpResponse == null || httpResponse.statusCode() != 200) && expires.isAfter(Instant.now())) {
            try {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(new URI(authEndpoint + "/oauth/token"))
                        .header("content-type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(mapToFormString(parameters)))
                        .build();
                httpResponse = HttpClient.newHttpClient()
                        .send(httpRequest, HttpResponse.BodyHandlers.ofString());

                switch (httpResponse.statusCode()) {
                    case 200:
                        break;
                    case 403:
                        Thread.sleep(oAuthDeviceCodeResponse.getInterval() * 1000);
                        break;
                    default:
                        log.error("Failed to request an authentication token, got http status code {}", httpResponse.statusCode());
                        return null;
                }
            } catch (Exception e) {
                log.error("Failed to request an authentication token", e);
                return null;
            }
        }
        if (httpResponse == null || httpResponse.statusCode() != 200) {
            out.error("Failed to authorize");
            return null;
        }

        OAuthTokenResponse response;
        try {
            response = objectMapper.readValue(httpResponse.body(), OAuthTokenResponse.class);
        } catch (IOException e) {
            log.error(String.format("The device token response was not the expected format \"%s\"", httpResponse.body()), e);
            return null;
        }
        return response;
    }

    private String mapToFormString(HashMap<String, String> parameters) {
        return parameters.keySet().stream()
                .map(key -> key + "=" + URLEncoder.encode(parameters.get(key), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
