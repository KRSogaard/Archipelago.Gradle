package build.archipelago.authservice.services.auth.models;

import build.archipelago.authservice.services.DBK;
import build.archipelago.common.dynamodb.AV;
import lombok.*;

import java.time.Instant;

@Value
@Builder
public class DeviceCode {
    private String deviceCode;
    private String userCode;
    private String scopes;
    private Instant expires;
    private String userId;
    private String clientId;
    private Instant updatedAt;
}
