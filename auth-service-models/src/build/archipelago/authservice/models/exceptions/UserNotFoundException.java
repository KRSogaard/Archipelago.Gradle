package build.archipelago.authservice.models.exceptions;

import lombok.*;

@Getter
public class UserNotFoundException extends Exception {
    private String email;

    public UserNotFoundException(String email) {
        super("The user " + email + " was not found");
        this.email = email;
    }
}
