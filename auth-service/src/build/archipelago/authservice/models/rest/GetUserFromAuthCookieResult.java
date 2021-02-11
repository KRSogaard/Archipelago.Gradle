package build.archipelago.authservice.models.rest;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GetUserFromAuthCookieResult {
    private String userId;
}
