package build.archipelago.authservice.services.clients;

import build.archipelago.authservice.services.clients.eceptions.ClientNotFoundException;

public interface ClientService {
    Client getClient(String clientId) throws ClientNotFoundException;

}
