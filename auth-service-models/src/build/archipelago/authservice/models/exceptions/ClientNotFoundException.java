package build.archipelago.authservice.models.exceptions;

import lombok.*;

@Getter
public class ClientNotFoundException extends Exception {
    private final String clientId;
    public ClientNotFoundException(String clientId) {
        super("Client " + clientId + " was not found");
        this.clientId = clientId;
    }
}
