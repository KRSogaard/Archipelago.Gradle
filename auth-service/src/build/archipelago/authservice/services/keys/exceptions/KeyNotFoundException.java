package build.archipelago.authservice.services.keys.exceptions;

public class KeyNotFoundException extends Exception {
    private String kid;

    public KeyNotFoundException(String kid) {
        super("The key '" + kid + "' was not found");
        this.kid = kid;
    }
}
