package build.archipelago.authservice.models.rest;

import build.archipelago.authservice.models.client.*;
import lombok.*;

@Builder
@Value
public class LogInRestResponse {
    private String authToken;

    public LogInResponse toInternal() {
        return LogInResponse.builder()
                .authToken(getAuthToken())
                .build();
    }
}
