package build.archipelago.authservice.services.auth.exceptions;

public class DeviceCodeNotFoundException extends Exception {
    private String deviceCode;

    public DeviceCodeNotFoundException(String deviceCode) {
        super("The device code '" + deviceCode + "' was not found, or was expired");
        this.deviceCode = deviceCode;
    }
}
