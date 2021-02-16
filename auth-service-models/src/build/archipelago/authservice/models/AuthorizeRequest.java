package build.archipelago.authservice.models;

import com.google.common.base.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthorizeRequest {
    private String responseType;
    private String responseMode;
    private String clientId;
    private String redirectUri;
    private String scope;
    private String state;
    private String nonce;

    public void validate() {
        if (Strings.isNullOrEmpty(responseType)) {
            throw new IllegalArgumentException("response_type is required");
        }
        if (Strings.isNullOrEmpty(responseMode)) {
            throw new IllegalArgumentException("response_mode is required");
        }
        if (Strings.isNullOrEmpty(clientId)) {
            throw new IllegalArgumentException("client_id is required");
        }
        if (Strings.isNullOrEmpty(redirectUri)) {
            throw new IllegalArgumentException("redirect_uri is required");
        }
    }
}
