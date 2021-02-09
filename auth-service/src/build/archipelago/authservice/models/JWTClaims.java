package build.archipelago.authservice.models;

import com.google.common.base.*;
import lombok.*;

import java.time.Instant;
import java.util.*;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JWTClaims {
    private String issuer;
    private String subject;
    private String audience;
    private Instant issuedAt;
    private Instant expires;
    private String scope;
    private Map<String, String> otherClaims;

    public static JWTClaimsBuilder builder() {
        return new JWTClaimsBuilder();
    }

    public Map<String, Object> getClaims() {
        Map<String, Object> claims = new HashMap<>();
        if (!Strings.isNullOrEmpty(issuer)) {
            claims.put("iss", issuer);
        }
        if (!Strings.isNullOrEmpty(subject)) {
            claims.put("sub", subject);
        }
        if (!Strings.isNullOrEmpty(audience)) {
            claims.put("aud", audience);
        }
        if (issuedAt != null) {
            claims.put("iat", issuedAt.getEpochSecond());
        }
        if (expires != null) {
            claims.put("ext", expires.getEpochSecond());
        }
        if (!Strings.isNullOrEmpty(issuer)) {
            claims.put("scope", scope);
        }
        for (String key : otherClaims.keySet()) {
            claims.put(key, otherClaims.get(key));
        }

        return claims;
    }

    public static final class JWTClaimsBuilder {
        private String issuer;
        private String subject;
        private String audience;
        private Instant issuedAt;
        private Instant expires;
        private String scope;
        private final Map<String, String> otherClaims;

        private JWTClaimsBuilder() {
            otherClaims = new HashMap<>();
        }

        public JWTClaimsBuilder withIssuer(String issuer) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(issuer));
            this.issuer = issuer;
            return this;
        }

        public JWTClaimsBuilder withSubject(String subject) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(subject));
            this.subject = subject;
            return this;
        }

        public JWTClaimsBuilder withAudience(String audience) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(audience));
            this.audience = audience;
            return this;
        }

        public JWTClaimsBuilder withIssuedAt(Instant issuedAt) {
            Preconditions.checkArgument(issuedAt != null);
            this.issuedAt = issuedAt;
            return this;
        }

        public JWTClaimsBuilder withExpires(Instant expires) {
            Preconditions.checkArgument(expires != null);
            this.expires = expires;
            return this;
        }

        public JWTClaimsBuilder withScope(String scope) {
            Preconditions.checkArgument(scope != null);
            this.scope = scope;
            return this;
        }

        public JWTClaimsBuilder withOClaim(String key, String value) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
            Preconditions.checkArgument(!Strings.isNullOrEmpty(value));
            this.otherClaims.put(key.trim(), value.trim());
            return this;
        }

        public JWTClaims build() {
            return new JWTClaims(issuer, subject, audience, issuedAt, expires, scope, otherClaims);
        }
    }
}
