package build.archipelago.harbor.models.auth;

import build.archipelago.authservice.models.client.*;
import lombok.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
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

    public LogInRequest toInternal() {
        return LogInRequest.builder()
                .responseType(this.getResponseType())
                .responseMode(this.getResponseMode())
                .clientId(this.getClientId())
                .redirectUri(this.getRedirectUri())
                .scope(this.getScope())
                .state(this.getState())
                .nonce(this.getNonce())
                .email(this.getEmail())
                .password(this.getPassword())
                .build();
    }
}
