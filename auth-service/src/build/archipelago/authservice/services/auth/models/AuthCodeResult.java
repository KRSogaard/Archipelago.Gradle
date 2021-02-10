package build.archipelago.authservice.services.auth.models;

import lombok.*;

import java.time.Instant;

@Builder
@Data
public class AuthCodeResult {
    private String code;
    private String userId;
    private String clientId;
    private String redirectURI;
    private String scopes;
    private Instant created;
    private Instant expires;
}
