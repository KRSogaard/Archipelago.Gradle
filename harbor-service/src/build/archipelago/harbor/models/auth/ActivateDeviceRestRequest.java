package build.archipelago.harbor.models.auth;

import lombok.*;

@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ActivateDeviceRestRequest {
    private String userCode;
}
