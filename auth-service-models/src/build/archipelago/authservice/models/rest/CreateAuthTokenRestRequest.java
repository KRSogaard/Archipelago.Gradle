package build.archipelago.authservice.models.rest;

import build.archipelago.authservice.models.*;
import build.archipelago.authservice.models.client.*;
import com.google.common.base.*;
import lombok.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CreateAuthTokenRestRequest {
    private String responseType;
    private String responseMode;
    private String clientId;
    private String redirectUri;
    private String scope;
    private String state;
    private String nonce;

    public AuthorizeRequest toInternal() {
        return AuthorizeRequest.builder()
                .responseType(responseType)
                .responseMode(responseMode)
                .clientId(clientId)
                .redirectUri(redirectUri)
                .scope(scope)
                .state(state)
                .nonce(nonce)
                .build();
    }

    public static CreateAuthTokenRestRequest from(AuthorizeRequest request) {
        return CreateAuthTokenRestRequest.builder()
                .responseType(request.getResponseType())
                .responseMode(request.getResponseMode())
                .clientId(request.getClientId())
                .redirectUri(request.getRedirectUri())
                .scope(request.getScope())
                .state(request.getState())
                .nonce(request.getNonce())
                .build();
    }

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
