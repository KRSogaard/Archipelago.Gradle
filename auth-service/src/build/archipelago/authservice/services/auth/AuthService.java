package build.archipelago.authservice.services.auth;

import build.archipelago.authservice.models.AuthorizeRequest;
import build.archipelago.authservice.services.auth.exceptions.*;
import build.archipelago.authservice.services.auth.models.AuthCodeResult;


public interface AuthService {
    String authenticate(String email, String password);

    String getUserFromAuthCode(String authCookieToken) throws UserNotFoundException;

    String createAuthToken(String userId, AuthorizeRequest request);

    AuthCodeResult getRequestFromAuthToken(String code);
}
