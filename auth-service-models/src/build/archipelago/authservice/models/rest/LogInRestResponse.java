package build.archipelago.authservice.models.rest;

import build.archipelago.authservice.models.client.*;
import lombok.*;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class LogInRestResponse {
    private String authToken;

    public LogInResponse toInternal() {
        return LogInResponse.builder()
                .authToken(getAuthToken())
                .build();
    }
}
