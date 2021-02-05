package build.archipelago.authservice.models.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.*;
import lombok.*;
import org.springframework.web.bind.annotation.RequestParam;

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

    private String email;
    private String password;

    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (Strings.isNullOrEmpty(response_type)) {
            errors.add("response_type is required");
        }
        if (Strings.isNullOrEmpty(response_mode)) {
            errors.add("response_mode is required");
        }
        if (Strings.isNullOrEmpty(client_id)) {
            errors.add("client_id is required");
        }
        if (Strings.isNullOrEmpty(redirect_uri)) {
            errors.add("redirect_uri is required");
        }

        return errors;
    }
}
