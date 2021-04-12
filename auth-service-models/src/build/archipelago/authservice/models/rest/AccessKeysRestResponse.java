package build.archipelago.authservice.models.rest;

import build.archipelago.authservice.models.AccessKey;
import lombok.*;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AccessKeysRestResponse  {
    private List<AccessKeyRestResponse> accessKeys;

    public static AccessKeysRestResponse from(List<AccessKey> keys) {
        return AccessKeysRestResponse.builder()
                .accessKeys(keys.stream().map(AccessKeyRestResponse::from).collect(Collectors.toList()))
                .build();
    }

    public List<AccessKey> toInternal() {
        return accessKeys.stream().map(AccessKeyRestResponse::toInternal).collect(Collectors.toList());
    }
}
