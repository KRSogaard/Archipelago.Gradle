package build.archipelago.authservice.models.rest;

import build.archipelago.authservice.models.AccessKey;
import build.archipelago.authservice.models.client.LogInResponse;
import lombok.*;

import java.time.Instant;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AccessKeyRestResponse {
    private String username;
    private String key;
    private String accountId;
    private Long created;
    private String scope;
    private Long lastUsed;

    public static AccessKeyRestResponse from(AccessKey key) {
        return AccessKeyRestResponse.builder()
                .username(key.getUsername())
                .key(key.getKey())
                .accountId(key.getAccountId())
                .created(key.getCreated().toEpochMilli())
                .lastUsed(key.getLastUsed() == null ? null : key.getLastUsed().toEpochMilli())
                .scope(key.getScope())
                .build();
    }

    public AccessKey toInternal() {
        return AccessKey.builder()
                .username(getUsername())
                .key(getKey())
                .accountId(getAccountId())
                .created(Instant.ofEpochMilli(getCreated()))
                .lastUsed(lastUsed == null ? null : Instant.ofEpochMilli(getLastUsed()))
                .scope(getScope())
                .build();
    }
}
