package build.archipelago.maui.core.auth;

import build.archipelago.common.exceptions.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class AuthServiceImpl implements AuthService {

    private com.fasterxml.jackson.databind.ObjectMapper objectMapper
            = new com.fasterxml.jackson.databind.ObjectMapper();

    private String clientId;
    private String authEndpoint;

    public AuthServiceImpl(String clientId,
                           String authEndpoint) {
        this.clientId = clientId;
        this.authEndpoint = authEndpoint;
    }

    @Override
    public OAuthDeviceCodeResponse getDeviceCode() {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", clientId);
        parameters.put("scope", "openid profile email");

        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(new URI(authEndpoint + "/oauth2/device_authorization"))
                    .header("content-type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(mapToFormString(parameters)))
                    .timeout(Duration.ofSeconds(1))
                    .build();
            httpResponse = HttpClient.newHttpClient()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (200 != httpResponse.statusCode()) {
                log.error("Failed to request an authentication token, got http status code {}", httpResponse.statusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to request an authentication token. Request url was:" + authEndpoint + "/oauth2/device_authorization", e);
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

    @Override
    public OAuthTokenResponse getToken(OAuthDeviceCodeResponse oAuthDeviceCodeResponse) {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", clientId);
        parameters.put("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
        parameters.put("device_code", oAuthDeviceCodeResponse.getDeviceCode());

        Instant expires = Instant.now().plusSeconds(oAuthDeviceCodeResponse.getExpiresIn());
        HttpResponse<String> httpResponse = null;
        while ((httpResponse == null || httpResponse.statusCode() != 200) && expires.isAfter(Instant.now())) {
            try {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(new URI(authEndpoint + "/oauth2/token"))
                        .header("content-type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(mapToFormString(parameters)))
                        .timeout(Duration.ofSeconds(1))
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
                return null;
            }
        }
        if (httpResponse == null || httpResponse.statusCode() != 200) {
            log.error("Failed to authorize");
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

    @Override
    public OAuthTokenResponse getToken(String accessKey, String accessKeyToken) {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", clientId);
        parameters.put("grant_type", "client_credentials");

        HttpResponse<String> httpResponse = null;
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(new URI(authEndpoint + "/oauth2/token"))
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("Authorization","Basic " + Base64.getEncoder().encodeToString((accessKey + ":" + accessKeyToken).getBytes(StandardCharsets.UTF_8)))
                    .POST(HttpRequest.BodyPublishers.ofString(mapToFormString(parameters)))
                    .timeout(Duration.ofSeconds(1))
                    .build();
            httpResponse = HttpClient.newHttpClient()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString());

            switch (httpResponse.statusCode()) {
                case 200:
                    return objectMapper.readValue(httpResponse.body(), OAuthTokenResponse.class);
                case 401:
                case 403:
                    log.error("Auth server returned unauthorized");
                    return null;
                default:
                    log.error("Failed to request an authentication token, got http status code {}", httpResponse.statusCode());
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String mapToFormString(HashMap<String, String> parameters) {
        return parameters.keySet().stream()
                .map(key -> key + "=" + URLEncoder.encode(parameters.get(key), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    @Override
    public boolean isTokenExpired(String token) {
        String[] split = token.split("\\.");
        if (split.length != 3) {
            throw new RuntimeException("Invalid JWT provided: '"+ token+ "'");
        }

        String decoded = new String(Base64.getDecoder().decode(split[1]));
        try {
            Map<String, Object> map = objectMapper.readValue(decoded, Map.class);
            if (!map.containsKey("ext")) {
                throw new RuntimeException("Invalid JWT provided: '"+ token+ "'");
            }
            Long ext = Long.valueOf(map.get("ext").toString());
            return ext < Instant.now().getEpochSecond();
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT provided: '"+ token+ "'");
        }
    }

    @Override
    public OAuthTokenResponse getTokenFromRefreshToken(String refreshToken) {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", clientId);
        parameters.put("grant_type", "refresh_token");
        parameters.put("refresh_token", refreshToken);

        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(new URI(authEndpoint + "/oauth2/token"))
                    .header("content-type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(mapToFormString(parameters)))
                    .timeout(Duration.ofSeconds(1))
                    .build();
            log.trace("Sending auth request to '{}/oauth2/token'", authEndpoint);
            httpResponse = HttpClient.newHttpClient()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString());

            switch (httpResponse.statusCode()) {
                case 200:
                    return objectMapper.readValue(httpResponse.body(), OAuthTokenResponse.class);
                case 401:
                case 403:
                    throw new UnauthorizedException();
                default:
                    log.error("Failed to request an authentication token, got http status code {}", httpResponse.statusCode());
                    return null;
            }
        } catch (Exception e) {
            log.error("Failed to authorize", e);
            throw new UnauthorizedException();
        }
    }
}
