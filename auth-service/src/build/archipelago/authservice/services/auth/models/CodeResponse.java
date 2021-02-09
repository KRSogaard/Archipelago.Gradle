package build.archipelago.authservice.services.auth.models;

import lombok.*;

import java.time.Instant;

@Value
@Builder
public class CodeResponse {
    private String code;
    private Instant expires;
}
