package build.archipelago.authservice.models.rest;

import build.archipelago.authservice.models.*;
import build.archipelago.authservice.models.client.*;
import com.google.common.base.*;
import lombok.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LogInRestRequest {
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

    public static LogInRestRequest from(LogInRequest request) {
        return LogInRestRequest.builder()
                .responseType(request.getResponseType())
                .responseMode(request.getResponseMode())
                .clientId(request.getClientId())
                .redirectUri(request.getRedirectUri())
                .scope(request.getScope())
                .state(request.getState())
                .nonce(request.getNonce())
                .email(request.getEmail())
                .password(request.getPassword())
                .build();
    }

    public AuthorizeRequest toInternal() {
        return AuthorizeRequest.builder()
                .responseType(getResponseType())
                .responseMode(getResponseMode())
                .clientId(getClientId())
                .redirectUri(getRedirectUri())
                .scope(getScope())
                .state(getState())
                .nonce(getNonce())
                .build();
    }
}
