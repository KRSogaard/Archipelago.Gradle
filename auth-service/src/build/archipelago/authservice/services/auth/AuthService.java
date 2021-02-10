package build.archipelago.authservice.services.auth;

import build.archipelago.authservice.models.*;
import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.authservice.services.auth.models.*;
import build.archipelago.authservice.services.users.exceptions.*;


public interface AuthService {
    String createAuthToken(String userId, AuthorizeRequest request);
    AuthCodeResult getRequestFromAuthToken(String code) throws TokenNotFoundException, TokenExpiredException;
    String getUserFromAuthCookie(String authCookie) throws UserNotFoundException, TokenExpiredException, TokenNotFoundException;
    CodeResponse createAuthCookie(String userId);
    DeviceCodeResponse createDeviceCode(String clientId, String scope);
    DeviceCode getDeviceCode(String userCode) throws TokenNotFoundException, TokenExpiredException;
    void updateDeviceCode(String userCode, String userId);
    DeviceCode getDeviceCodeByDeviceId(String deviceCode) throws TokenNotFoundException, TokenExpiredException;
    void removeDeviceCode(String userCode);

    void clearExpiredCodes();
}
