package build.archipelago.authservice.controllers;

import build.archipelago.authservice.models.*;
import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.authservice.models.rest.*;
import build.archipelago.authservice.services.auth.*;
import build.archipelago.authservice.services.auth.models.AuthCodeResult;
import build.archipelago.authservice.services.clients.*;
import build.archipelago.authservice.services.clients.eceptions.*;
import build.archipelago.authservice.services.auth.exceptions.*;
import build.archipelago.authservice.services.keys.*;
import build.archipelago.authservice.utils.*;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import javax.ws.rs.*;

@RestController
@RequestMapping("oauth2")
@Slf4j
public class OAuth2Controller {

    private long refreshTokenMaxAge = 60 * 60 * 24 * 30;
    private long accessTokenMaxAge = 60 * 60 * 3;

    private ImmutableList<String> allowedScopes = ImmutableList.of("openId", "email", "profile", "other", "todo");
    private ImmutableList<String> allowedResponseMode = ImmutableList.of("query", "fragment");
    private AuthService authService;
    private ClientService clientService;
    private KeyService keyService;

    public OAuth2Controller(AuthService authService, ClientService clientService, KeyService keyService) {
        this.authService = authService;
        this.clientService = clientService;
        this.keyService = keyService;
    }

    @GetMapping("authorize")
    public ResponseEntity<String> getAuthorize(@CookieValue(value = "archipelago.auth", required = false) String authCookieToken,
                                               @RequestParam(name = "response_type") String responseType,
                                               @RequestParam(name = "response_mode") String responseMode,
                                               @RequestParam(name = "client_id") String clientId,
                                               @RequestParam(name = "redirect_uri") String redirectUri,
                                               @RequestParam(name = "scope") String scope,
                                               @RequestParam(name = "state") String state,
                                               @RequestParam(name = "nonce") String nonce) {
        AuthorizeRequest request = AuthorizeRequest.builder()
                .responseType(responseType)
                .responseMode(responseMode)
                .clientId(clientId)
                .redirectUri(redirectUri)
                .scope(scope)
                .state(state)
                .nonce(nonce)
                .build();
        try {
            // Todo this throws errors
            request.validate();

            if (!responseType.equalsIgnoreCase("code")) {
                return ResponseUtil.redirect("/error?error=invalid_request&state=" + state);
            }

            Client client = clientService.getClient(clientId);
            if (!UrlUtil.checkRedirectUrl(redirectUri, client.getAllowedRedirects())) {
                return ResponseUtil.redirect("/error?error=unauthorized_client&state=" + state);
            }

            List<String> scopes = splitScopes(scope);
            List<String> invalidScopes = getInvalidScope(scopes, client.getAllowedScopes());
            if (invalidScopes.size() > 0) {
                return createErrorResponse("invalid_scope", request, Map.of("invalidScopes", invalidScopes));
            }

            if (!isResponseModeOk(responseMode)) {
                return createErrorResponse("unsupported_response_type", request, null);
            }

            if (!Strings.isNullOrEmpty(authCookieToken)) {
                try {
                    String userId = authService.getUserFromAuthCode(authCookieToken);
                    String authToken = authService.createAuthToken(userId, request);

                    StringBuilder redirectUrlBuilder = new StringBuilder();
                    redirectUrlBuilder.append(redirectUri);
                    if (responseMode.equalsIgnoreCase("query")) {
                        redirectUrlBuilder.append("?");
                    } else {
                        redirectUrlBuilder.append("#");
                    }
                    redirectUrlBuilder.append("code=").append(authToken);
                    if (!Strings.isNullOrEmpty(state)) {
                        redirectUrlBuilder.append("&state=").append(state);
                    }
                    return ResponseUtil.redirect(redirectUrlBuilder.toString());
                } catch (UserNotFoundException exp) {
                    log.warn("The auth cookie was not found in our database");
                }
            }

            // TODO: This is for testing
            if (state.equalsIgnoreCase("kasper-test")) {
                String authToken = authService.createAuthToken("HMZSm2cvELCGRybg7BeA", request);
                StringBuilder redirectUrlBuilder = new StringBuilder();
                redirectUrlBuilder.append(redirectUri);
                if (responseMode.equalsIgnoreCase("query")) {
                    redirectUrlBuilder.append("?");
                } else {
                    redirectUrlBuilder.append("#");
                }
                redirectUrlBuilder.append("code=").append(authToken);
                if (!Strings.isNullOrEmpty(state)) {
                    redirectUrlBuilder.append("&state=").append(state);
                }
                return ResponseUtil.redirect(redirectUrlBuilder.toString());
            }
            // TODO: Remove here

            return createLoginUrl(request);
        } catch (ClientNotFoundException exp) {
            return createErrorResponse("unauthorized_client", request, null);
        } catch (IllegalArgumentException exp) {
            return createErrorResponse("invalid_request", request, null);
        }
    }

    private ResponseEntity<String> createLoginUrl(AuthorizeRequest request) {
        StringBuilder redirectUrlBuilder = new StringBuilder();
        redirectUrlBuilder.append("/login?response_type=");
        redirectUrlBuilder.append(StringEscapeUtils.escapeHtml4(request.getResponseType()));
        if (!Strings.isNullOrEmpty(request.getResponseType())) {
            redirectUrlBuilder.append("&response_mode=");
            redirectUrlBuilder.append(StringEscapeUtils.escapeHtml4(request.getResponseType()));
        }
        redirectUrlBuilder.append("&client_id=");
        redirectUrlBuilder.append(StringEscapeUtils.escapeHtml4(request.getClientId()));
        redirectUrlBuilder.append("&redirect_uri=");
        redirectUrlBuilder.append(StringEscapeUtils.escapeHtml4(request.getRedirectUri()));
        redirectUrlBuilder.append("&scope=");
        redirectUrlBuilder.append(StringEscapeUtils.escapeHtml4(request.getScope()));
        if (!Strings.isNullOrEmpty(request.getState())) {
            redirectUrlBuilder.append("&state=");
            redirectUrlBuilder.append(StringEscapeUtils.escapeHtml4(request.getState()));
        }
        if (!Strings.isNullOrEmpty(request.getNonce())) {
            redirectUrlBuilder.append("&nonce=");
            redirectUrlBuilder.append(StringEscapeUtils.escapeHtml4(request.getNonce()));
        }

        return ResponseUtil.redirect(redirectUrlBuilder.toString());
    }

    private ResponseEntity<String> createErrorResponse(String error, AuthorizeRequest request, Map<String, Object> data) {
        StringBuilder redirectUrlBuilder = new StringBuilder();
        redirectUrlBuilder.append("/error?error=");
        redirectUrlBuilder.append(error);
        if (!Strings.isNullOrEmpty(request.getState())) {
            redirectUrlBuilder.append("&state=");
            redirectUrlBuilder.append(StringEscapeUtils.escapeHtml4(request.getState()));
        }
        if (data != null) {
            for (String key : data.keySet()) {
                if (data.get(key) instanceof List) {
                    redirectUrlBuilder.append("&");
                    redirectUrlBuilder.append(StringEscapeUtils.escapeHtml4(key));
                    redirectUrlBuilder.append("=");
                    redirectUrlBuilder.append(((List<String>) data.get(key)).stream()
                        .map(StringEscapeUtils::escapeHtml4)
                        .collect(Collectors.joining(",")));
                } else if (data.get(key) instanceof String) {
                    redirectUrlBuilder.append("&");
                    redirectUrlBuilder.append(StringEscapeUtils.escapeHtml4(key));
                    redirectUrlBuilder.append("=");
                    redirectUrlBuilder.append(StringEscapeUtils.escapeHtml4((String) data.get(key)));
                }
            }
        }
        return ResponseUtil.redirect(redirectUrlBuilder.toString());
    }

    private List<String> splitScopes(String scope) {
        return List.of(scope.split(" "));
    }

    private List<String> getInvalidScope(List<String> scopes, List<String> clientAllowedScopes) {
        List<String> invalidScopes = new ArrayList<>();
        invalidScopes.addAll(scopes.stream().filter(s -> allowedScopes.stream().noneMatch(q -> q.equalsIgnoreCase(s))).collect(Collectors.toList()));
        invalidScopes.addAll(scopes.stream().filter(s -> clientAllowedScopes.stream().noneMatch(q -> q.equalsIgnoreCase(s))).collect(Collectors.toList()));
        return invalidScopes.stream().distinct().collect(Collectors.toList());
    }

    private boolean isResponseModeOk(String responseMode) {
        return allowedResponseMode.stream().anyMatch(responseMode::equalsIgnoreCase);
    }

    @PostMapping(path = "token",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> postToken(
           @RequestHeader(value = "authorization", required = false) String authorizationHeader,
           @RequestParam MultiValueMap<String, String> formData
    ) {
        TokenRestRequest request = TokenRestRequest.builder()
                .code(formData.getFirst("code"))
                .clientId(formData.getFirst("client_id"))
                .redirectUri(formData.getFirst("redirect_uri"))
                .grantType(formData.getFirst("grant_type"))
                .refreshToken(formData.getFirst("refresh_token"))
                .build();
        if (!"authorization_code".equalsIgnoreCase(request.getGrantType()) &&
            !"refresh_token".equalsIgnoreCase(request.getGrantType())) {
            return ResponseUtil.invalidGrantType();
        }

        Client client;
        try {
            if (!Strings.isNullOrEmpty(authorizationHeader)) {
                UserCredential authHeader = HeaderUtil.extractCredential(authorizationHeader);
                if (authHeader == null) {
                    return ResponseUtil.invalidClient();
                }
                client = clientService.getClient(authHeader.getUsername());
                if (!client.getClientSecret().equalsIgnoreCase(authHeader.getPassword())) {
                    log.warn("Client password was incorrect");
                    return ResponseUtil.invalidClient();
                }
            } else {
                if (Strings.isNullOrEmpty(request.getClientId())) {
                    return ResponseUtil.invalidClient();
                }
                client = clientService.getClient(request.getClientId());
                if (!Strings.isNullOrEmpty(client.getClientSecret())) {
                    log.warn("Client '{}' has a secret but it was not provided", request.getClientId());
                    return ResponseUtil.invalidClient();
                }
            }
        } catch (ClientNotFoundException exp) {
            log.warn("The client not found '{}'", exp.getClientId());
            return ResponseUtil.invalidClient();
        }

        UserAndScopes userAndScopes;
        if ("authorization_code".equalsIgnoreCase(request.getGrantType())) {
            userAndScopes = getUserFromNewAuth(request);
        } else if ("refresh_token".equalsIgnoreCase(request.getGrantType())) {
            userAndScopes = getUserFromRefreshToken(request);
        } else {
            log.error("Unknown grant type '{}'", request.getGrantType());
            return ResponseUtil.invalidGrantType();
        }

        TokenRestResponse response = new TokenRestResponse();

        Map<String, Object> accessTokenBody = createAccessToken(userAndScopes.getUserId(), userAndScopes.getScopes(), client.getClientId());
        response.setAccessToken(createJWT(accessTokenBody));

        if (userAndScopes.getScopes().contains("openid")) {
            Map<String, Object> openIdTokenBody = createOpenIdToken(userAndScopes.getUserId(), userAndScopes.getScopes(), client.getClientId());
            response.setIdToken(createJWT(openIdTokenBody));
        }

        if ("authorization_code".equalsIgnoreCase(request.getGrantType())) {
            Map<String, Object> refreshTokenBody = createRefreshToken(userAndScopes.getUserId(), userAndScopes.getScopes(), client.getClientId());
            response.setRefreshToken(createJWT(refreshTokenBody));
        }

        response.setTokenType("Bearer");
        response.setExpiresIn(3600);

        return ResponseEntity.status(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .body(JSONUtil.serialize(response));
    }

    private String createJWT(Map<String, Object> body) {
        KeyDetails details = keyService.getSigningKey();

        return Jwts.builder()
                .setHeaderParam(JwsHeader.KEY_ID, details.getKeyId())
                .setHeaderParam(JwsHeader.ALGORITHM, details.getAlgorithm())
                .setClaims(body)
                .signWith(details.getPrivatKey())
                .compact();
    }

    private Map<String, Object> createRefreshToken(String userId, List<String> scopes, String clientId) {
        Map<String, Object> token = new HashMap<>();
        token.put("iss", "https://auth.archipelago.build");
        token.put("sub", userId);
        token.put("aud", clientId);
        token.put("iat", Instant.now().getEpochSecond());
        token.put("exp", Instant.now().plusSeconds(refreshTokenMaxAge).getEpochSecond());
        token.put("scopes", String.join(" ", scopes));
        return token;
    }

    private Map<String, Object> createAccessToken(String userId, List<String> scopes, String clientId) {
        Map<String, Object> token = new HashMap<>();
        token.put("iss", "https://auth.archipelago.build");
        token.put("sub", userId);
        token.put("aud", clientId);
        token.put("iat", Instant.now().getEpochSecond());
        token.put("exp", Instant.now().plusSeconds(accessTokenMaxAge).getEpochSecond());
        token.put("scopes", String.join(" ", scopes));
        return token;
    }

    private Map<String, Object> createOpenIdToken(String userId, List<String> scopes, String clientId) {
        return null;
    }

    private UserAndScopes getUserFromRefreshToken(TokenRestRequest request) {
        if (Strings.isNullOrEmpty(request.getRefreshToken())) {
            throw new IllegalArgumentException("refresh_token is missing");
        }
        // TODO: verify and parse token
        return null;
    }

    private UserAndScopes getUserFromNewAuth(TokenRestRequest request) {
        AuthCodeResult codeResult = authService.getRequestFromAuthToken(request.getCode());
        if (codeResult == null || Instant.now().isAfter(codeResult.getExpires())) {
            log.debug("Auth code was not found or the code has expired");
            throw new UnauthorizedAuthTokenException();
        }

        if (!codeResult.getRedirectURI().equalsIgnoreCase(request.getRedirectUri())) {
            log.warn("Request uri did not match '{}' during the authorize request and '{}' during the token",
                    codeResult.getRedirectURI(), request.getRedirectUri());
            throw new InvalidRedirectException(request.getRedirectUri());
        }

        return UserAndScopes.builder()
                .userId(codeResult.getUserId())
                .scopes(ScopeUtils.getScopes(codeResult.getScopes()))
                .build();
    }

    @GetMapping("callback")
    public ResponseEntity<Object> getCallback() {
        return null;
    }

    @GetMapping(".well-known/jwks.json")
    public JWKSRestResponse getJWKS() {
        List<JWKKey> keys = keyService.getActiveKeys();
        List<JWKRestResponse> responseKeys = new ArrayList<>();
        for (JWKKey k : keys) {
            responseKeys.add(JWKRestResponse.builder()
                    .alg(k.getAlg())
                    .use("sig")
                    .kid(k.getKid())
                    .kty(k.getKty())
                    .n(k.getPublicKey())
                    .build());
        }
        return JWKSRestResponse.builder()
                .keys(responseKeys)
                .build();
    }
}
