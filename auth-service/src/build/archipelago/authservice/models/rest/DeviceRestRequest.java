package build.archipelago.authservice.models.rest;

import lombok.*;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class DeviceRestRequest {
    private String user_code;
    private String email;
    private String password;
}
