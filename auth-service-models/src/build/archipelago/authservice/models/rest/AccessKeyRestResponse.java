package build.archipelago.authservice.models.rest;

import build.archipelago.authservice.models.AccessKey;
import lombok.*;

import java.time.Instant;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AccessKeyRestResponse {
    private String username;
    private String token;
    private String accountId;
    private Long created;
    private String scope;
    private Long lastUsed;

    public static AccessKeyRestResponse from(AccessKey key) {
        return AccessKeyRestResponse.builder()
                .username(key.getUsername())
                .token(key.getToken())
                .accountId(key.getAccountId())
                .created(key.getCreated().toEpochMilli())
                .lastUsed(key.getLastUsed() == null ? null : key.getLastUsed().toEpochMilli())
                .scope(key.getScope())
                .build();
    }

    public AccessKey toInternal() {
        return AccessKey.builder()
                .username(getUsername())
                .token(getToken())
                .accountId(getAccountId())
                .created(Instant.ofEpochMilli(getCreated()))
                .lastUsed(lastUsed == null ? null : Instant.ofEpochMilli(getLastUsed()))
                .scope(getScope())
                .build();
    }
}
