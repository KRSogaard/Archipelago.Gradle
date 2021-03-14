package build.archipelago.authservice.client;

import build.archipelago.authservice.models.*;
import build.archipelago.authservice.models.client.*;
import build.archipelago.authservice.models.exceptions.*;

import java.util.List;

public interface AuthClient {
    void register(RegisterRequest registerRequest) throws UserExistsException;
    boolean isUserInAccount(String accountId, String userId);
    List<String> getAccountsForUser(String userId);

    LogInResponse login(LogInRequest logInRequest) throws UserNotFoundException;
    void device(ActivateDeviceRequest request) throws TokenNotFoundException, TokenExpiredException;
    LogInResponse createAuthToken(String userId, AuthorizeRequest request);
}
