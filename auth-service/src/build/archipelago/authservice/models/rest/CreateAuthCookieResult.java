package build.archipelago.authservice.models.rest;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateAuthCookieResult {
    private String cookieCode;
    private long expires;
}
