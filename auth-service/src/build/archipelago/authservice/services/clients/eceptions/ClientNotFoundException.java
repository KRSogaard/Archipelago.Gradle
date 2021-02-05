package build.archipelago.authservice.services.clients.eceptions;

import lombok.Data;

@Data
public class ClientNotFoundException extends Exception {
    private String clientId;
    public ClientNotFoundException(String clientId) {
        super("Client " + clientId + " was not found");
        this.clientId = clientId;
    }
}
