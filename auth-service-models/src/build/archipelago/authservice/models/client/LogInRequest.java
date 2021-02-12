package build.archipelago.authservice.models.client;

import com.google.common.base.*;
import lombok.*;

@Value
@Builder
public class LogInRequest {
    private String responseType;
    private String responseMode;
    private String clientId;
    private String redirectUri;
    private String scope;
    private String state;
    private String nonce;
    private String email;
    private String password;

    public void validate() {
        if (Strings.isNullOrEmpty(getResponseType())) {
            throw new IllegalArgumentException("response_type is required");
        } else if (!"code".equalsIgnoreCase(getResponseType())) {
            throw new IllegalArgumentException("Only the \"code\" response_type is supported");
        }
        if (Strings.isNullOrEmpty(getClientId())) {
            throw new IllegalArgumentException("client_id is required");
        }
        if (Strings.isNullOrEmpty(getRedirectUri())) {
            throw new IllegalArgumentException("redirect_uri is required");
        } else if (!getRedirectUri().startsWith("http")) {
            throw new IllegalArgumentException("redirect_uri is malformed");
        }
    }
}
