package build.archipelago.authservice.models.exceptions;

import lombok.*;

@Getter
public class KeyNotFoundException extends Exception {
    private String kid;

    public KeyNotFoundException(String kid) {
        super("The key '" + kid + "' was not found");
        this.kid = kid;
    }
}
