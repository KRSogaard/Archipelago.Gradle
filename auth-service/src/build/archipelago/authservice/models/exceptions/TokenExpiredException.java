package build.archipelago.authservice.models.exceptions;

public class TokenExpiredException extends Exception {
    private String deviceCode;

    public TokenExpiredException() {
        super("The token has expired");
    }
    public TokenExpiredException(String deviceCode) {
        super("The token '" + deviceCode + "' has expired");
        this.deviceCode = deviceCode;
    }
}
