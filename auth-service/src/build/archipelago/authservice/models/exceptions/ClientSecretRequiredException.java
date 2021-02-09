package build.archipelago.authservice.models.exceptions;

import lombok.Value;

@Value
public class ClientSecretRequiredException extends Exception {
    private final String clientId;

    public ClientSecretRequiredException(String clientId) {
        super("Client " + clientId + " requires a secret");
        this.clientId = clientId;
    }
}
