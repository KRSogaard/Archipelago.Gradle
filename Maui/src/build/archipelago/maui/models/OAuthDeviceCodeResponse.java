package build.archipelago.maui.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OAuthDeviceCodeResponse {
    @JsonProperty("device_code")
    private String deviceCode;
    @JsonProperty("user_code")
    private String userCode;
    @JsonProperty("verification_uri")
    private String verificationUri;
    @JsonProperty("expires_in")
    private Integer expiresIn;
    private Integer interval;
    @JsonProperty("verification_uri_complete")
    private String verificationUriComplete;
}
