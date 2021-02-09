package build.archipelago.authservice.controllers;

import build.archipelago.authservice.models.*;
import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.authservice.models.rest.*;
import build.archipelago.authservice.services.auth.*;
import build.archipelago.authservice.services.auth.models.*;
import build.archipelago.authservice.services.clients.*;
import build.archipelago.authservice.services.clients.eceptions.*;
import build.archipelago.authservice.services.keys.exceptions.KeyNotFoundException;
import build.archipelago.authservice.services.users.exceptions.*;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static build.archipelago.authservice.controllers.Constants.AUTH_COOKIE;

@RestController
@RequestMapping("oauth2")
@Slf4j
public class OAuth2Controller {

    private long refreshTokenMaxAge;
    private long accessTokenMaxAge;
    private String authUrl;

    private ImmutableList<String> allowedScopes = ImmutableList.of("openId", "email", "profile", "other", "todo");
    private ImmutableList<String> allowedResponseMode = ImmutableList.of("query", "fragment");
    private AuthService authService;
    private ClientService clientService;
    private KeyService keyService;

    public OAuth2Controller(AuthService authService, ClientService clientService, KeyService keyService,
                            String authUrl,
                            long refreshTokenMaxAge,
                            long accessTokenMaxAge) {
        this.authService = authService;
        this.clientService = clientService;
        this.keyService = keyService;

        this.authUrl = authUrl;
        this.refreshTokenMaxAge = refreshTokenMaxAge;
        this.accessTokenMaxAge = accessTokenMaxAge;
    }

    @GetMapping("authorize")
    public ResponseEntity<String> getAuthorize(@CookieValue(value = AUTH_COOKIE, required = false) String authCookieToken,
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
                log.info("The redirect url '{}' was not allowed for client '{}'", redirectUri, client.getClientId());
                return ResponseUtil.redirect("/error?error=unauthorized_client&state=" + state);
            }
            if (!Strings.isNullOrEmpty(client.getClientSecret())) {
                log.info("The client '{}' has a secret, but it was not provided or was invalid", client.getClientId());
                return ResponseUtil.redirect("/error?error=unauthorized_client&state=" + state);
            }

            List<String> scopes = ScopeUtils.getScopes(scope);
            List<String> invalidScopes = getInvalidScope(scopes, client.getAllowedScopes());
            if (invalidScopes.size() > 0) {
                return createErrorResponse("invalid_scope", request, Map.of("invalidScopes", invalidScopes));
            }

            if (!isResponseModeOk(responseMode)) {
                return createErrorResponse("unsupported_response_type", request, null);
            }

            if (!Strings.isNullOrEmpty(authCookieToken)) {
                try {
                    String userId = authService.getUserFromAuthCookie(authCookieToken);
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
        redirectUrlBuilder.append(encodeValue(request.getResponseType()));
        if (!Strings.isNullOrEmpty(request.getResponseType())) {
            redirectUrlBuilder.append("&response_mode=");
            redirectUrlBuilder.append(encodeValue(request.getResponseMode()));
        }
        redirectUrlBuilder.append("&client_id=");
        redirectUrlBuilder.append(encodeValue(request.getClientId()));
        redirectUrlBuilder.append("&redirect_uri=");
        redirectUrlBuilder.append(encodeValue(request.getRedirectUri()));
        redirectUrlBuilder.append("&scope=");
        redirectUrlBuilder.append(encodeValue(request.getScope()));
        if (!Strings.isNullOrEmpty(request.getState())) {
            redirectUrlBuilder.append("&state=");
            redirectUrlBuilder.append(encodeValue(request.getState()));
        }
        if (!Strings.isNullOrEmpty(request.getNonce())) {
            redirectUrlBuilder.append("&nonce=");
            redirectUrlBuilder.append(encodeValue(request.getNonce()));
        }

        return ResponseUtil.redirect(redirectUrlBuilder.toString());
    }
    private String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private ResponseEntity<String> createErrorResponse(String error, AuthorizeRequest request, Map<String, Object> data) {
        StringBuilder redirectUrlBuilder = new StringBuilder();
        redirectUrlBuilder.append("/auth-error?error=");
        redirectUrlBuilder.append(error);
        if (!Strings.isNullOrEmpty(request.getState())) {
            redirectUrlBuilder.append("&state=");
            redirectUrlBuilder.append(encodeValue(request.getState()));
        }
        if (data != null) {
            for (String key : data.keySet()) {
                if (data.get(key) instanceof List) {
                    redirectUrlBuilder.append("&");
                    redirectUrlBuilder.append(encodeValue(key));
                    redirectUrlBuilder.append("=");
                    redirectUrlBuilder.append(((List<String>) data.get(key)).stream()
                        .map(StringEscapeUtils::escapeHtml4)
                        .collect(Collectors.joining(",")));
                } else if (data.get(key) instanceof String) {
                    redirectUrlBuilder.append("&");
                    redirectUrlBuilder.append(encodeValue(key));
                    redirectUrlBuilder.append("=");
                    redirectUrlBuilder.append(encodeValue((String) data.get(key)));
                }
            }
        }
        return ResponseUtil.redirect(redirectUrlBuilder.toString());
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
    ) throws KeyNotFoundException, TokenNotValidException, ClientNotFoundException, ClientSecretRequiredException {
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

        Client client = getClient(request.getClientId(), authorizationHeader);

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
        response.setTokenType("Bearer");
        response.setExpiresIn(3600);
        response.setAccessToken(createJWT(JWTClaims.builder()
                .withIssuer("https://auth.archipelago.build")
                .withSubject(userAndScopes.getUserId())
                .withAudience(client.getClientId())
                .withIssuedAt(Instant.now())
                .withExpires(Instant.now().plusSeconds(accessTokenMaxAge))
                .withScope(String.join(" ", userAndScopes.getScopes()))
                .build().getClaims()));
        response.setRefreshToken(createJWT(JWTClaims.builder()
                .withIssuer("https://auth.archipelago.build")
                .withSubject(userAndScopes.getUserId())
                .withAudience(client.getClientId())
                .withIssuedAt(Instant.now())
                .withExpires(Instant.now().plusSeconds(refreshTokenMaxAge))
                .withScope(String.join(" ", userAndScopes.getScopes()))
                .build().getClaims()));

        if (userAndScopes.getScopes().contains("openid")) {
            Map<String, Object> openIdTokenBody = createOpenIdToken(userAndScopes.getUserId(), userAndScopes.getScopes(), client.getClientId());
            response.setIdToken(createJWT(openIdTokenBody));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .body(JSONUtil.serialize(response));
    }

    private Client getClient(String requestClientId, String authorizationHeader) throws ClientNotFoundException,
            ClientSecretRequiredException {
        Client client;
        String clientId = null;
        String clientSecret = null;

        if (!Strings.isNullOrEmpty(authorizationHeader)) {
            UserCredential authHeader = HeaderUtil.extractCredential(authorizationHeader);
            if (authHeader == null || Strings.isNullOrEmpty(authHeader.getUsername())) {
                throw new ClientNotFoundException(null);
            }
            clientId = authHeader.getUsername();
            clientSecret = authHeader.getPassword();
        } else {
            if (Strings.isNullOrEmpty(requestClientId)) {
                throw new ClientNotFoundException(null);
            }
            clientId = requestClientId;
            clientSecret = null; // Client secret should only be given as auth header
        }

        client = clientService.getClient(clientId);
        if (!Strings.isNullOrEmpty(client.getClientSecret()) && Strings.isNullOrEmpty(clientSecret)) {
            log.warn("Client '{}' has a secret but it was not provided", clientId);
            throw new ClientSecretRequiredException(client.getClientSecret());
        }
        if (!client.getClientSecret().equalsIgnoreCase(clientSecret)) {
            log.warn("Client secret was incorrect");
            throw new ClientNotFoundException(clientId);
        }
        return client;
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

    private Map<String, Object> createOpenIdToken(String userId, List<String> scopes, String clientId) {
        return null;
    }

    private UserAndScopes getUserFromRefreshToken(TokenRestRequest request) throws KeyNotFoundException, TokenNotValidException {
        if (Strings.isNullOrEmpty(request.getRefreshToken())) {
            throw new IllegalArgumentException("refresh_token is missing");
        }

        Map<String, String> tokenHead = JWTUtil.getHeader(request.getRefreshToken());
        if (!tokenHead.containsKey("kid") || Strings.isNullOrEmpty(tokenHead.get("kid"))) {
            throw new UnauthorizedAuthTokenException();
        }
        String kid = tokenHead.get("kid");
        KeyDetails details = keyService.getKey(kid);
        Jws<Claims> token = null;
        try {
            token = Jwts.parserBuilder().setSigningKey(details.getPublicKey()).build().parseClaimsJws(request.getRefreshToken());
        } catch (JwtException e) {
            log.info("The refresh token failed validation");
            throw new TokenNotValidException();
        }

        List<String> currentScopes = ScopeUtils.getScopes(token.getBody().get("scope", String.class));
        List<String> requestedScopes = ScopeUtils.getScopes(request.getScope());
        List<String> newScopes = ScopeUtils.ensureNoNewScopes(currentScopes, requestedScopes);

        return UserAndScopes.builder()
                .userId(token.getBody().getSubject())
                .scopes(newScopes)
                .build();
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

        if (!Strings.isNullOrEmpty(request.getScope())) {
            log.warn("Authorization_code request contained a scope request, this is not allowed");
            throw new IllegalArgumentException("scope is not allowed on authorization_code requests");
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

    @PostMapping("/device_authorization")
    public DeviceActivationRestResponse deviceActivation(
            @RequestHeader(value = "authorization", required = false) String authorizationHeader,
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "scope") String scope) throws ClientSecretRequiredException, ClientNotFoundException {
        if (Strings.isNullOrEmpty(clientId)) {
            throw new IllegalArgumentException("client_id is required");
        }
        // This will throw exception if the client auth is not valid
        getClient(clientId, authorizationHeader);

        CodeResponse deviceCode = authService.createDeviceCode(clientId, scope);

        return DeviceActivationRestResponse.builder()
                .deviceCode(deviceCode.getCode())
                .expiresIn(deviceCode.getExpires().getEpochSecond() - Instant.now().getEpochSecond())
                .verificationUri(authUrl + "/device")
                .verificationUriComplete(authUrl + "/device?user_code=" + deviceCode.getCode())
                .interval(5)
                .build();
    }
}
