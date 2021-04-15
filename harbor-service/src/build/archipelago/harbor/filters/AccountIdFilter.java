package build.archipelago.harbor.filters;

import build.archipelago.authservice.client.AuthClient;
import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.harbor.controllers.*;
import com.fasterxml.jackson.databind.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.*;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.*;
import java.security.*;
import java.security.spec.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.servlet.*;
import javax.servlet.http.*;

@Component
@Slf4j
public class AccountIdFilter implements Filter {
    private static final ObjectMapper mapper = new ObjectMapper();
    public static final String AccountIdKey = "account-id";
    public static final String UserIdKey = "user-id";
    private static Map<String, PublicKey> keyMap;
    private static Instant lastUpdate;
    private static Cache<String, CacheItem> accessTokenCache;
    private static Cache<String, PublicKey> keyCache;

    private String authEndpoint;
    private AuthClient authClient;

    @Autowired
    public AccountIdFilter(@Value("${frontend-oauth.auth-url}") String authEndpoint, AuthClient authClient) {
        log.info("Using OAuth endpoint '{}' for Account Id Filter", authEndpoint);
        keyMap = new HashMap<>();
        this.authEndpoint = authEndpoint;
        this.authClient = authClient;

        accessTokenCache = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
        keyCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .maximumSize(100)
                .build();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        log.debug("Setting account id for request {}", httpServletRequest.getRequestURL());

        try {
            String accessToken = getAccessToken(httpServletRequest);
            CacheItem item = accessTokenCache.getIfPresent(accessToken);
            if (item != null) {
                if (Instant.now().isAfter(item.getExpires())) {
                    log.warn("Token has expired, it expired at {}, time is now {}", item.getExpires(), Instant.now());
                    throw new UnauthorizedException();
                }
                log.debug("User is identified as {}, account {}", item.getUserId(), item.getAccountId());
                request.setAttribute(AccountIdKey, item.getAccountId());
                request.setAttribute(UserIdKey, item.getUserId());
                chain.doFilter(request, response);
                return;
            }

            String kid = getKid(accessToken);
            log.debug("Using KID: '{}'", kid);
            PublicKey publicKey = keyCache.get(kid.toLowerCase(), k -> getPublicKey(kid));
            Jws<Claims> claims = getClaims(publicKey, accessToken);
            Instant expires = checkExpired(claims);
            String userId = claims.getBody().getSubject();
            log.debug("User claims to be: '{}'", userId);
            if (Strings.isNullOrEmpty(userId) && "client".equalsIgnoreCase((String)claims.getBody().get("access_type"))) {
                log.debug("No user id, but it is a client credentials access, searching the header");
                userId = httpServletRequest.getHeader("user_id");
                if (Strings.isNullOrEmpty(userId)) {
                    log.warn("Request client id: '{}'", userId);
                    throw new UnauthorizedException();
                }
            }

            if (Strings.isNullOrEmpty(userId)) {
                log.warn("Claim did not contain a user id");
                throw new UnauthorizedException();
            }
            String accountId = getAccountId(userId);
            log.info("Mapped user '{}' to account id '{}'", userId, accountId);

            accessTokenCache.put(accessToken, new CacheItem(userId, accountId, expires));
            request.setAttribute(AccountIdKey, accountId);
            request.setAttribute(UserIdKey, userId);
            chain.doFilter(request, response);
        } catch (UnauthorizedException exp) {
            String url = httpServletRequest.getRequestURL().toString();
            if (url.endsWith("/auth/login") ||
                url.endsWith("/auth/register") ||
                url.endsWith(HealthController.HEALTH_PATH)) {
                log.debug("Authentication was not valid. But the request was to an unprotected path");
                // this is not a blocked url;
                chain.doFilter(request, response);
                return;
            }
            log.warn("Authentication was not valid. Returning 401");
            httpServletResponse.sendError(401, "Invalid or expired token");
            return;
        }
    }

    private Instant checkExpired(Jws<Claims> claims) {
        if (!claims.getBody().containsKey("ext")) {
            log.warn("JWT token did not contain the ext element");
            throw new UnauthorizedException();
        }

        try {
            Long ext = claims.getBody().get("ext", Long.class);
            if (ext < Instant.now().getEpochSecond()) {
                log.debug("JWT was expired");
                throw new UnauthorizedException();
            }
            return Instant.ofEpochSecond(ext);
        } catch (RequiredTypeException exp) {
            log.warn("JWT ext was not a number: '{}'", claims.getBody().get("ext"));
            throw new UnauthorizedException();
        }
    }

    private String getAccountId(String userId) throws UnauthorizedException {
        List<String> accountIds = authClient.getAccountsForUser(userId);
        if (accountIds.size() == 0) {
            throw new UnauthorizedException();
        }
        return accountIds.get(0);
    }

    private Jws<Claims> getClaims(PublicKey publicKey, String accessToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(accessToken);
        } catch (JwtException exp) {
            throw new UnauthorizedException();
        }
    }

    private String getAccessToken(HttpServletRequest request) throws UnauthorizedException {
        List<String> headers = Collections.list(request.getHeaders("Authorization"));
        String authorization;
        if (headers.size() != 1) {
            log.warn("Authorization header was was not provided");
            throw new UnauthorizedException();
        }
        String header = headers.get(0);
        if (!header.startsWith("Bearer ")) {
            log.warn("Authorization header was was not a bearer token");
            throw new UnauthorizedException();
        }
        String[] split = header.split(" ", 2);
        if (split.length != 2 || Strings.isNullOrEmpty(split[1])) {
            log.warn("Authorization header was was not a valid bearer token");
            throw new UnauthorizedException();
        }
        return split[1];
    }

    private PublicKey getPublicKey(String kid) {
        if (keyMap == null || !keyMap.containsKey(kid) || lastUpdate.plusSeconds(60 * 60).isBefore(Instant.now())) {
            updateKeyMap();
        }
        if (!keyMap.containsKey(kid)) {
            throw new UnauthorizedException();
        }
        return keyMap.get(kid);
    }


    public String getKid(String accessToken) {
        try {
            String header = accessToken.split("\\.")[0];
            String decode = new String(Base64.getDecoder().decode(header));
            JsonNode node = mapper.readTree(decode);
            return node.get("kid").asText();
        } catch (Exception exp) {
            throw new UnauthorizedException();
        }
    }

    private void updateKeyMap() {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");

            Map<String, PublicKey> newArray = new HashMap<>();
            HttpClient client = HttpClient
                    .newBuilder()
                    .build();

            URI uri = new URI(authEndpoint + "/.well-known/jwks.json");
            log.info("Fetching well knows tokens from '{}'", uri.toString());
            HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                    .header("content-type", "application/json")
                    .header("accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> restResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            String keys = restResponse.body();
            JsonNode node = mapper.readTree(keys);
            for (JsonNode key : node.get("keys")) {
                String keyKid = key.get("kid").asText();
                String keyN = key.get("n").asText();
                PublicKey pubicKey = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(keyN.getBytes(StandardCharsets.UTF_8))));
                newArray.put(keyKid, pubicKey);
            }
            keyMap = newArray;
            lastUpdate = Instant.now();
        } catch (Exception e) {
            log.error("Failed to get keys");
            throw new RuntimeException(e);
        }
    }

    private class CacheItem {
        private String userId;
        private String accountId;

        private Instant expires;

        public CacheItem(String userId, String accountId, Instant expires) {
            this.userId = userId;
            this.accountId = accountId;
            this.expires = expires;
        }

        public String getUserId() {
            return userId;
        }

        public String getAccountId() {
            return accountId;
        }

        public Instant getExpires() {
            return expires;
        }
    }
}
