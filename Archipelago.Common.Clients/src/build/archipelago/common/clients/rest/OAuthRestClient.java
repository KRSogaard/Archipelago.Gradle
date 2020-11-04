package build.archipelago.common.clients.rest;

import build.archipelago.common.exceptions.UnauthorizedException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.net.http.*;
import java.time.Instant;

@Slf4j
public abstract class OAuthRestClient {

    private String oauthToken;
    private Instant expires;

    private String tokenUrl;
    private String clientId;
    private String clientSecret;
    private String audience;
    private String grantType;


    protected HttpClient client;
    protected String baseUrl;
    protected com.fasterxml.jackson.databind.ObjectMapper objectMapper
            = new com.fasterxml.jackson.databind.ObjectMapper();

    public OAuthRestClient(String baseUrl, String tokenUrl, String clientId, String clientSecret, String audience) {
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.audience = audience;
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
    public OAuthRestClient(String baseUrl, String tokenUrl, String oauthToken, String audience) {
        this.tokenUrl = tokenUrl;
        this.oauthToken = oauthToken;
        this.expires = Instant.MAX;
        this.audience = audience;

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
        return addOauth(getBaseJSONRequest(baseUrl + url));
    }
    protected HttpRequest.Builder addOauth(HttpRequest.Builder request) throws UnauthorizedException {
        if (oauthToken == null || Instant.now().isAfter(expires)) {
            renewToken();
        }

        return request.header("authorization", "Bearer " + oauthToken);
    }

    protected void renewToken() throws UnauthorizedException {
        if (clientId == null) {
            throw new RuntimeException("Can only renew token with client id and secret credentials");
        }
        String body = String.format("{\"client_id\":\"%s\"," +
                "\"client_secret\":\"%s\"," +
                "\"audience\":\"%s\"," +
                "\"grant_type\":\"%s\"}", clientId, clientSecret, audience, grantType);
        try {
            HttpRequest request = getBaseJSONRequest(tokenUrl)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
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
}