package build.archipelago.authservice.models.client;

import com.google.common.base.*;
import lombok.*;

@Builder
@Value
public class ActivateDeviceRequest {
    private String userCode;
    private String userId;

    public void validate() {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(userCode));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(userId));
    }
}
