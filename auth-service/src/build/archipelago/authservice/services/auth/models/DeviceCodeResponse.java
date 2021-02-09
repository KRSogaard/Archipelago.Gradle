package build.archipelago.authservice.services.auth.models;

import lombok.*;

import java.time.Instant;

@Value
@Builder
public class DeviceCodeResponse {
    private String deviceCode;
    private String userCode;
    private Instant expires;
}
