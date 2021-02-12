package build.archipelago.authservice.models.exceptions;

import lombok.*;

@Getter
public class InvalidRedirectException extends RuntimeException {

    private final String redirectUri;

    public InvalidRedirectException(String redirect_uri) {
        super("The redirect uri '" + redirect_uri + "' was invalid");
        this.redirectUri = redirect_uri;
    }
}
