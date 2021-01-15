package build.archipelago.buildserver.api.client.rest;

import build.archipelago.buildserver.api.client.BuildServerAPIClient;
import build.archipelago.buildserver.models.client.Builds;
import build.archipelago.buildserver.models.rest.BuildRestResponse;
import build.archipelago.buildserver.models.rest.BuildsRestResponse;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.clients.rest.OAuthRestClient;
import build.archipelago.common.exceptions.UnauthorizedException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Collectors;

public class RestBuildServerAPIClient extends OAuthRestClient implements BuildServerAPIClient {

    private static final String OAUTH2_SCOPES = "http://buildserver-api.archipelago.build/read http://buildserver-api.archipelago.build/create";
    private static final String OAUTH2_TOKENURL = "https://archipelago.auth.us-west-2.amazoncognito.com/oauth2/token";

    public RestBuildServerAPIClient(String endpoint, String clientId, String clientSecret) {
        super(endpoint, OAUTH2_TOKENURL, clientId, clientSecret, OAUTH2_SCOPES);
    }

    public Builds getBuilds(String accountId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));

        BuildsRestResponse response;
        HttpResponse<String> restResponse;
        try {
            HttpRequest request = getOAuthRequest("/account/" + accountId + "/build")
                    .GET()
                    .build();
            restResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        switch (restResponse.statusCode()) {
            case 401:
                throw new UnauthorizedException();
            case 200: // Ok
                try {
                    response = objectMapper.readValue(restResponse.body(), BuildsRestResponse.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                throw new RuntimeException("Unknown response " + restResponse.statusCode());
        }

        return Builds.builder()
                .waitingBuilds(response.getWaitingBuilds().stream()
                        .map(BuildRestResponse::toArchipelagoBuild).collect(Collectors.toList()))
                .processingBuilds(response.getProcessingBuilds().stream()
                        .map(BuildRestResponse::toArchipelagoBuild).collect(Collectors.toList()))
                .pastBuilds(response.getPastBuilds().stream()
                        .map(BuildRestResponse::toArchipelagoBuild).collect(Collectors.toList()))
                .build();
    }
}
