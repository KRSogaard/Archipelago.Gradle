package build.archipelago.authservice.models;

import com.google.common.base.Strings;
import lombok.*;

import java.util.*;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthorizeRequest {
    private String responseType;
    private String responseMode;
    private String clientId;
    private String redirectUri;
    private String scope;
    private String state;
    private String nonce;

    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (Strings.isNullOrEmpty(responseType)) {
            errors.add("response_type is required");
        }
        if (Strings.isNullOrEmpty(responseMode)) {
            errors.add("response_mode is required");
        }
        if (Strings.isNullOrEmpty(clientId)) {
            errors.add("client_id is required");
        }
        if (Strings.isNullOrEmpty(redirectUri)) {
            errors.add("redirect_uri is required");
        }

        return errors;
    }
}
