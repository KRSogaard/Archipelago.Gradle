package build.archipelago.authservice.models.exceptions;

import lombok.Getter;

@Getter
public class ClientSecretRequiredException extends Exception {
    private final String clientId;

    public ClientSecretRequiredException(String clientId) {
        super("Client " + clientId + " requires a secret");
        this.clientId = clientId;
    }
}
