package build.archipelago.authservice.services.keys.models;

import lombok.*;

import java.security.*;
import java.time.Instant;

@Builder
@Value
public class KeyDetails {
    private String keyId;
    private PrivateKey privatKey;
    private PublicKey publicKey;
    private String algorithm;
    private String type;
    private Instant expiresAt;
}
