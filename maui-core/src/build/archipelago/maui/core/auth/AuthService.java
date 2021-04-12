package build.archipelago.maui.core.auth;

public interface AuthService {
    OAuthDeviceCodeResponse getDeviceCode();

    OAuthTokenResponse getToken(OAuthDeviceCodeResponse oAuthDeviceCodeResponse);
    OAuthTokenResponse getToken(String accessKey, String accessKeyToken);

    boolean isTokenExpired(String token);

    OAuthTokenResponse getTokenFromRefreshToken(String refreshToken);
}
