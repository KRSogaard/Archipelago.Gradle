package build.archipelago.authservice.models.rest;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@Builder
@Value
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class DeviceActivationRestResponse {
    private String deviceCode;
    private String userCode;
    private String verificationUri;
    private String verificationUriComplete;
    private long expiresIn;
    private int interval;
}
