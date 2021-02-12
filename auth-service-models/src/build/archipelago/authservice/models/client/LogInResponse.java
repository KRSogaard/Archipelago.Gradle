package build.archipelago.authservice.models.client;

import lombok.*;

@Builder
@Value
public class LogInResponse {
    private String authToken;
}
