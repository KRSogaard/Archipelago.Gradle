package build.archipelago.authservice.services.clients.eceptions;

import lombok.*;

@Getter
public class ClientNotFoundException extends Exception {
    private final String clientId;
    public ClientNotFoundException(String clientId) {
        super("Client " + clientId + " was not found");
        this.clientId = clientId;
    }
}
