package build.archipelago.authservice.models;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class AccessKey {
    private String username;
    private String key;
    private String accountId;
    private Instant created;
    private String scope;
    private Instant lastUsed;
}
