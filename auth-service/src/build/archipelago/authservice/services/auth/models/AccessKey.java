package build.archipelago.authservice.services.auth.models;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class AccessKey {
    private String username;
    private String key;
    private Instant created;
    private String scope;
    private Instant lastUsed;
}
