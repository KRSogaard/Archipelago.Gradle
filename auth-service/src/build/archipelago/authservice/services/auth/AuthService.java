package build.archipelago.authservice.services.auth;

import build.archipelago.authservice.models.*;
import build.archipelago.authservice.services.auth.exceptions.*;
import build.archipelago.authservice.services.auth.models.*;
import build.archipelago.authservice.services.users.exceptions.*;


public interface AuthService {
    String createAuthToken(String userId, AuthorizeRequest request);
    AuthCodeResult getRequestFromAuthToken(String code);
    String getUserFromAuthCookie(String authCookie) throws UserNotFoundException;
    CodeResponse createAuthCookie(String userId);
    DeviceCodeResponse createDeviceCode(String clientId, String scope);
    DeviceCode getDeviceCode(String userCode) throws DeviceCodeNotFoundException;
    void updateDeviceCode(String userCode, String userId);
}