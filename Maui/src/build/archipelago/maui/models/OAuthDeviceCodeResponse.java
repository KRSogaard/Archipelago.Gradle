package build.archipelago.maui.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
public class OAuthDeviceCodeResponse {
    @JsonProperty("device_code")
    private String deviceCode;
    @JsonProperty("user_code")
    private String userCode;
    @JsonProperty("verification_uri")
    private String verificationUri;
    @JsonProperty("expires_in")
    private int expiresIn;
    private int interval;
    @JsonProperty("verification_uri_complete")
    private int verificationUriComplete;
}
