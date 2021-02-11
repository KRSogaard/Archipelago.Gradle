package build.archipelago.authservice.models.rest;

import lombok.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateAuthCookieRequest {
    private String userId;
}
