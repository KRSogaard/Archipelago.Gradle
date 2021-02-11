package build.archipelago.authservice.services.users.models;

import lombok.*;

@Builder
@Value
public class UserModel {
    private String name;
    private String email;
    private String password;
}
