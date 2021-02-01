package build.archipelago.authservice.models;

import lombok.*;

@Builder
@Value
public class UserCredential {
    private String username;
    private String password;
}
