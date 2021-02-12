package build.archipelago.authservice.client;

import build.archipelago.authservice.models.client.*;
import build.archipelago.authservice.models.exceptions.*;

public interface AuthClient {
    LogInResponse login(LogInRequest logInRequest) throws UserNotFoundException;
    void device(ActivateDeviceRequest request) throws TokenNotFoundException, TokenExpiredException;
}
