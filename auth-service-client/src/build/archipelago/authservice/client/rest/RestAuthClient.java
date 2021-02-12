package build.archipelago.authservice.client.rest;

import build.archipelago.authservice.client.*;
import build.archipelago.authservice.models.client.*;
import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.authservice.models.rest.*;
import build.archipelago.common.clients.rest.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.rest.models.errors.*;
import com.google.common.base.*;
import lombok.extern.slf4j.*;

import java.net.http.*;

@Slf4j
public class RestAuthClient extends OAuthRestClient implements AuthClient {
    private static final String OAUTH2_SCOPES = "http://auth.archipelago.build/read http://auth.archipelago.build/write";
    private static final String OAUTH2_TOKENURL = "https://archipelago.auth.us-west-2.amazoncognito.com/oauth2/token";

    public RestAuthClient(String endpoint, String clientId, String clientSecret) {
        super(endpoint, OAUTH2_TOKENURL, clientId, clientSecret, OAUTH2_SCOPES);
    }

    @Override
    public LogInResponse login(LogInRequest logInRequest) throws UserNotFoundException {
        Preconditions.checkNotNull(logInRequest);
        logInRequest.validate();

        LogInRestRequest restRequest = LogInRestRequest.from(logInRequest);

        HttpResponse<String> restResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/auth/login")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .header("accept", "application/json")
                    .build();
            restResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call package service to create a package");
            throw new RuntimeException(e);
        }

        switch (restResponse.statusCode()) {
            case 200: // Ok
                LogInRestResponse response = this.parseOrThrow(restResponse.body(), LogInRestResponse.class);
                return response.toInternal();
            case 401:
            case 403:
                log.error("Got unauthorized response from auth service");
                throw new UnauthorizedException();
            case 404:
                log.warn("Got Not Found (404) response from auth service with body: " + restResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(restResponse.body());
                throw (UserNotFoundException) AuthExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(restResponse);
        }
    }

    @Override
    public void device(ActivateDeviceRequest request) throws TokenNotFoundException, TokenExpiredException {
        Preconditions.checkNotNull(request);
        request.validate();

        ActivateDeviceRestRequest restRequest = ActivateDeviceRestRequest.from(request);

        HttpResponse<String> restResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/auth/device")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .header("accept", "application/json")
                    .build();
            restResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call package service to create a package");
            throw new RuntimeException(e);
        }

        switch (restResponse.statusCode()) {
            case 200: // Ok
                return;
            case 401:
            case 403:
                try {
                    ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(restResponse.body());
                    throw (TokenExpiredException) AuthExceptionHandler.createException(problem);
                } catch (Exception exp) {
                    // This is fine
                }
                log.error("Got unauthorized response from auth service");
                throw new UnauthorizedException();
            case 404:
                log.warn("Got Not Found (404) response from auth service with body: " + restResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(restResponse.body());
                throw (TokenNotFoundException) AuthExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(restResponse);
        }
    }

    private RuntimeException logAndReturnExceptionForUnknownStatusCode(HttpResponse<String> restResponse) {
        log.error("Unknown response from auth service status code " + restResponse.statusCode() +
                " body: " + restResponse.body());
        return new RuntimeException("Unknown response from auth service status code " + restResponse.statusCode() +
                " body: " + restResponse.body());
    }
}
