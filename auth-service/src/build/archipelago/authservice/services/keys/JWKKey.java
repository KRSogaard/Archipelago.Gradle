package build.archipelago.authservice.services.keys;

import lombok.*;

import java.time.Instant;

@Builder
@Data
public class JWKKey {
    private String kid;
    private String privateKey;
    private String publicKey;
    private Instant expiresAt;
    private String alg;
    private String kty;
}
