package build.archipelago.authservice.models.rest;

import build.archipelago.authservice.models.client.*;
import lombok.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
<<<<<<< HEAD
=======
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
>>>>>>> origin/AuthApi
public class LogInRestResponse {
    private String authToken;

    public LogInResponse toInternal() {
        return LogInResponse.builder()
                .authToken(getAuthToken())
                .build();
    }
}
