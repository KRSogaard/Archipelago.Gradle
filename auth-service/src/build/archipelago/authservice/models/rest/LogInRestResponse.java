package build.archipelago.authservice.models.rest;

import lombok.*;

@Builder
@Value
public class LogInRestResponse {
    private String authToken;
}
