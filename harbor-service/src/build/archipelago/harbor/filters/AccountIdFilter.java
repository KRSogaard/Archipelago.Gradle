package build.archipelago.harbor.filters;

import build.archipelago.account.common.AccountService;
import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.harbor.controllers.*;
import com.fasterxml.jackson.databind.*;
import com.google.common.base.*;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.*;
import java.security.*;
import java.security.spec.*;
import java.time.*;
import java.util.*;
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

    private String authEndpoint;
    private AccountService accountService;

    @Autowired
    public AccountIdFilter(@Value("${oauth.auth-url}") String authEndpoint, AccountService accountService) {
        keyMap = new HashMap<>();
        this.authEndpoint = authEndpoint;
        this.accountService = accountService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        log.info("Setting account id");
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        try {
            String accessToken = getAccessToken(httpServletRequest);
            String kid = getKid(accessToken);
            PublicKey publicKey = getPublicKey(kid);
            Jws<Claims> claims = getClaims(publicKey, accessToken);
            String userId = claims.getBody().getSubject();
            if (Strings.isNullOrEmpty(userId)) {
                throw new UnauthorizedException();
            }
            String accountId = getAccountId(userId);

            request.setAttribute(AccountIdKey, accountId);
            request.setAttribute(UserIdKey, userId);
            chain.doFilter(request, response);
        } catch (UnauthorizedException exp) {
            String url = httpServletRequest.getRequestURL().toString();
            if (url.endsWith("/auth/login") ||
                url.endsWith(HealthController.HEALTH_PATH)) {
                // this is not a blocked url;
                chain.doFilter(request, response);
                return;
            }
            httpServletResponse.setStatus(401);
        }
    }

    private String getAccountId(String userId) {
        return accountService.getAccountIdForUser(userId);
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
            log.warn("Authorization was invalid");
            throw new UnauthorizedException();
        }
        String header = headers.get(0);
        if (!header.startsWith("Bearer ")) {
            throw new UnauthorizedException();
        }
        String[] split = header.split(" ", 2);
        if (split.length != 2 || Strings.isNullOrEmpty(split[1])) {
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
            HttpRequest httpRequest = HttpRequest.newBuilder(new URI(authEndpoint + "/.well-known/jwks.json"))
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

}
