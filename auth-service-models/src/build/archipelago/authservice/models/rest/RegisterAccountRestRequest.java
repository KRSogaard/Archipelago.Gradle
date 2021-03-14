package build.archipelago.authservice.models.rest;

import build.archipelago.authservice.models.client.RegisterRequest;
import lombok.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RegisterAccountRestRequest {
    private String name;
    private String email;
    private String password;

    public static RegisterAccountRestRequest from(RegisterRequest registerRequest) {
        return RegisterAccountRestRequest.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(registerRequest.getPassword())
                .build();
    }
}
