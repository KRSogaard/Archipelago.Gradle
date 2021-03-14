package build.archipelago.authservice.models.client;

import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RegisterRequest {
    private String name;
    private String email;
    private String password;

    public void validate() {
        if (Strings.isNullOrEmpty(getName())) {
            throw new IllegalArgumentException("name is required");
        }
        if (Strings.isNullOrEmpty(getEmail())) {
            throw new IllegalArgumentException("email is required");
        }
        if (Strings.isNullOrEmpty(getPassword())) {
            throw new IllegalArgumentException("password is required");
        }
    }
}
