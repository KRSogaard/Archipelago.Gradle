package build.archipelago.common.clients.rest;

import build.archipelago.common.exceptions.UnauthorizedException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class OAuthRestClient extends BaseRestClient {

    private String oauthToken;
    private Instant expires;

    private String tokenUrl;
    private String clientId;
    private String clientSecret;
    private String scopes;
    private String grantType;


    protected HttpClient client;
    protected String baseUrl;

    public OAuthRestClient(String baseUrl, String tokenUrl, String clientId, String clientSecret, String scopes) {
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scopes = scopes;
        this.grantType = "client_credentials";

        if (baseUrl.endsWith("/")) {
            this.baseUrl = baseUrl.substring(0, baseUrl.length() - 2);
        } else {
            this.baseUrl = baseUrl;
        }
        client = HttpClient
                .newBuilder()
                .build();
    }

    public OAuthRestClient(String baseUrl, String tokenUrl, String oauthToken, String scopes) {
        this.tokenUrl = tokenUrl;
        this.oauthToken = oauthToken;
        this.expires = Instant.MAX;
        this.scopes = scopes;

        if (baseUrl.endsWith("/")) {
            this.baseUrl = baseUrl.substring(0, baseUrl.length() - 2);
        } else {
            this.baseUrl = baseUrl;
        }
        client = HttpClient
                .newBuilder()
                .build();
    }

    private HttpRequest.Builder getBaseJSONRequest(String url) throws URISyntaxException {
        return HttpRequest.newBuilder(new URI(url))
                .header("content-type", "application/json")
                .header("accept", "application/json");
    }

    protected HttpRequest.Builder getOAuthRequest(String url) throws UnauthorizedException, URISyntaxException {
        log.debug("Creating OAuth request to " + baseUrl + url);
        return this.addOauth(this.getBaseJSONRequest(baseUrl + url));
    }

    protected HttpRequest.Builder addOauth(HttpRequest.Builder request) throws UnauthorizedException {
        if (oauthToken == null || Instant.now().isAfter(expires)) {
            this.renewToken();
        }

        return request.header("authorization", "Bearer " + oauthToken);
    }

    protected void renewToken() throws UnauthorizedException {
        if (clientId == null) {
            throw new RuntimeException("Can only renew token with client id and secret credentials");
        }

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", clientId);
        parameters.put("scope", scopes);
        parameters.put("grant_type", grantType);
        String form = parameters.keySet().stream()
                .map(key -> key + "=" + URLEncoder.encode(parameters.get(key), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        try {
            HttpRequest request = HttpRequest.newBuilder(new URI(tokenUrl))
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("accept", "*/*")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .setHeader("Authorization", basicAuth(clientId, clientSecret))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new UnauthorizedException();
            }
            JsonNode tokenJSON = objectMapper.readTree(response.body());
            oauthToken = tokenJSON.get("access_token").asText();
            int expiresIn = tokenJSON.get("expires_in").asInt();
            // We subtract 5, just to make sure it dose not expire during a call
            expires = Instant.now().plusSeconds(expiresIn).minusSeconds(5);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String basicAuth(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }
}
