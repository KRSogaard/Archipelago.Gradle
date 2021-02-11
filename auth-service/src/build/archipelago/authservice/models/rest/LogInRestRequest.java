package build.archipelago.authservice.models.rest;

import build.archipelago.authservice.models.AuthorizeRequest;
import com.google.common.base.Strings;
import lombok.*;

import java.util.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LogInRestRequest {
    private String responseType;
    private String responseNode;
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

    public AuthorizeRequest toInternal() {
        return AuthorizeRequest.builder()
                .responseType(getResponseType())
                .responseMode(getResponseNode())
                .clientId(getClientId())
                .redirectUri(getRedirectUri())
                .scope(getScope())
                .state(getState())
                .nonce(getNonce())
                .build();
    }
}
