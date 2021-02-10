package build.archipelago.authservice.services.clients;

import build.archipelago.authservice.services.clients.eceptions.ClientNotFoundException;
import build.archipelago.authservice.services.clients.models.*;

public interface ClientService {
    Client getClient(String clientId) throws ClientNotFoundException;
}
