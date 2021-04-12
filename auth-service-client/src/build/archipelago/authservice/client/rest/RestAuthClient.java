package build.archipelago.authservice.client.rest;

import build.archipelago.authservice.client.*;
import build.archipelago.authservice.models.*;
import build.archipelago.authservice.models.client.*;
import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.authservice.models.rest.*;
import build.archipelago.common.clients.rest.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.rest.models.errors.*;
import com.google.common.base.*;
import lombok.extern.slf4j.*;

import java.net.http.*;
import java.util.List;

@Slf4j
public class RestAuthClient extends OAuthRestClient implements AuthClient {
    private static final String OAUTH2_SCOPES = "http://auth.archipelago.build/read http://auth.archipelago.build/write";
    private static final String OAUTH2_TOKENURL = "https://archipelago.auth.us-west-2.amazoncognito.com/oauth2/token";

    public RestAuthClient(String endpoint, String clientId, String clientSecret) {
        super(endpoint, OAUTH2_TOKENURL, clientId, clientSecret, OAUTH2_SCOPES);
    }

    @Override
    public void register(RegisterRequest registerRequest) throws UserExistsException {
        Preconditions.checkNotNull(registerRequest);
        registerRequest.validate();

        RegisterAccountRestRequest restRequest = RegisterAccountRestRequest.from(registerRequest);

        HttpResponse<String> restResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/user/register")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .header("accept", "application/json")
                    .build();
            restResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call auth service to register the user");
            throw new RuntimeException(e);
        }

        switch (restResponse.statusCode()) {
            case 200: // Ok
                return;
            case 401:
            case 403:
                log.error("Got unauthorized response from auth service");
                throw new UnauthorizedException();
            case 409:
                log.warn("Got Conflict (409) response from auth service with body: " + restResponse.body());
                ProblemDetailRestResponse problem = ProblemDetailRestResponse.from(restResponse.body());
                throw (UserExistsException) AuthExceptionHandler.createException(problem);
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(restResponse);
        }
    }

    @Override
    public boolean isUserInAccount(String accountId, String userId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "User id is required");

        VerifyAccountMembershipRestRequest restRequest = VerifyAccountMembershipRestRequest.builder()
                .accountId(accountId)
                .userId(userId)
                .build();

        HttpResponse<String> restResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/account/verify-membership")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .header("accept", "application/json")
                    .build();
            restResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call auth service to check if user is in account");
            throw new RuntimeException(e);
        }

        switch (restResponse.statusCode()) {
            case 200:
                return true;
            case 401:
            case 403:
                log.error("Got unauthorized response from auth service");
                throw new UnauthorizedException();
            case 404:
                return false;
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(restResponse);
        }
    }

    @Override
    public List<String> getAccountsForUser(String userId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "User id is required");

        HttpResponse<String> restResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/account/user/" + userId)
                    .GET()
                    .header("accept", "application/json")
                    .build();
            restResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call auth service to get accounts for user");
            throw new RuntimeException(e);
        }

        switch (restResponse.statusCode()) {
            case 200:
                UserAccountsRestResult response = this.parseOrThrow(restResponse.body(), UserAccountsRestResult.class);
                return response.getAccounts();
            case 401:
            case 403:
                log.error("Got unauthorized response from auth service");
                throw new UnauthorizedException();
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(restResponse);
        }
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
            log.error("Got unknown error while trying to call auth service to login");
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
    public LogInResponse createAuthToken(String userId, AuthorizeRequest request) {
        Preconditions.checkNotNull(request);
        request.validate();

        CreateAuthTokenRestRequest restRequest = CreateAuthTokenRestRequest.from(request);

        HttpResponse<String> restResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/auth/user/" + userId + "/create-token")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(restRequest)))
                    .header("accept", "application/json")
                    .build();
            restResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call auth service to create an auth token");
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
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(restResponse);
        }
    }

    @Override
    public AccessKey createAccessKey(String accountId, String userId, String scopes) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));

        CreateAccessKeyRestRequest request = CreateAccessKeyRestRequest.builder()
                .userId(userId)
                .scope(scopes)
                .build();
        HttpResponse<String> restResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/accessKeys/" + accountId)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .header("accept", "application/json")
                    .build();
            restResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call auth service to create an access key");
            throw new RuntimeException(e);
        }

        switch (restResponse.statusCode()) {
            case 200: // Ok
                AccessKeyRestResponse response = this.parseOrThrow(restResponse.body(), AccessKeyRestResponse.class);
                return response.toInternal();
            case 401:
            case 403:
                log.error("Got unauthorized response from auth service");
                throw new UnauthorizedException();
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(restResponse);
        }
    }

    @Override
    public List<AccessKey> getAccessKeys(String accountId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");

        HttpResponse<String> restResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/accessKeys/" + accountId)
                    .GET()
                    .header("accept", "application/json")
                    .build();
            restResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call auth service to get accounts for user");
            throw new RuntimeException(e);
        }

        switch (restResponse.statusCode()) {
            case 200:
                AccessKeysRestResponse response = this.parseOrThrow(restResponse.body(), AccessKeysRestResponse.class);
                return response.toInternal();
            case 401:
            case 403:
                log.error("Got unauthorized response from auth service");
                throw new UnauthorizedException();
            default:
                throw this.logAndReturnExceptionForUnknownStatusCode(restResponse);
        }
    }

    @Override
    public void deleteAccessKey(String accountId, String username) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(username));

        HttpResponse<String> restResponse;
        try {
            HttpRequest httpRequest = this.getOAuthRequest("/accessKeys/" + accountId + "/" + username)
                    .DELETE()
                    .build();
            restResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (UnauthorizedException exp) {
            log.error("Was unable to auth with the auth server, did not get to call the client", exp);
            throw exp;
        } catch (Exception e) {
            log.error("Got unknown error while trying to call auth service to create an access key");
            throw new RuntimeException(e);
        }

        switch (restResponse.statusCode()) {
            case 200: // Ok
                return;
            case 401:
            case 403:
                log.error("Got unauthorized response from auth service");
                throw new UnauthorizedException();
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
            log.error("Got unknown error while trying to call auth service to activate device");
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
