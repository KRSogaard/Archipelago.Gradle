package build.archipelago.maui.builder.clients;

import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.maui.core.auth.AuthService;
import build.archipelago.maui.core.auth.OAuthDeviceCodeResponse;
import build.archipelago.maui.core.auth.OAuthTokenResponse;

public class UnauthorizedAuthService implements AuthService {
    @Override
    public OAuthDeviceCodeResponse getDeviceCode() {
        throw new UnauthorizedException();
    }

    @Override
    public OAuthTokenResponse getToken(OAuthDeviceCodeResponse oAuthDeviceCodeResponse) {
        throw new UnauthorizedException();
    }

    @Override
    public boolean isTokenExpired(String token) {
        return true;
    }

    @Override
    public OAuthTokenResponse getTokenFromRefreshToken(String refreshToken) {
        throw new UnauthorizedException();
    }
}
