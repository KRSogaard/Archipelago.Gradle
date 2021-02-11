package build.archipelago.authservice.models.rest;

import build.archipelago.authservice.models.*;
import com.google.common.base.*;
import lombok.*;

import java.util.*;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class AuthorizeRestRequest {
    private String response_type;
    private String response_mode;
    private String client_id;
    private String redirect_uri;
    private String scope;
    private String state;
    private String nonce;

    private String email;
    private String password;

    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (Strings.isNullOrEmpty(response_type)) {
            errors.add("response_type is required");
        } else if (!"code".equalsIgnoreCase(response_type)) {
            errors.add("Only the \"code\" response_type is supported");
        }
        if (Strings.isNullOrEmpty(client_id)) {
            errors.add("client_id is required");
        }
        if (Strings.isNullOrEmpty(redirect_uri)) {
            errors.add("redirect_uri is required");
        } else if (!redirect_uri.startsWith("http")) {
            errors.add("redirect_uri is malformed");
        }

        return errors;
    }

    public AuthorizeRequest toInternal() {
        return AuthorizeRequest.builder()
                .responseType(response_type)
                .responseMode(response_mode)
                .clientId(client_id)
                .redirectUri(redirect_uri)
                .scope(scope)
                .state(state)
                .build();
    }
}
