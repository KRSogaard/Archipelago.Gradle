package build.archipelago.maui.core.auth;

public interface AuthService {
    OAuthDeviceCodeResponse getDeviceCode();

    OAuthTokenResponse getToken(OAuthDeviceCodeResponse oAuthDeviceCodeResponse);

    boolean isTokenExpired(String token);

    OAuthTokenResponse getTokenFromRefreshToken(String refreshToken);
}
