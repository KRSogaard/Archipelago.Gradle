package build.archipelago.authservice.models.rest;

import lombok.*;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class CreateAccountModel {
    private String response_type;
    private String response_mode;
    private String client_id;
    private String redirect_uri;
    private String scope;
    private String state;
    private String nonce;

    private String name;
    private String email;
    private String password;
    private String password_repeat;
}
