package build.archipelago.packageservice.core.delegates.createPackage;

import com.google.common.base.*;
import lombok.*;

@Data
@Builder
public class CreatePackageDelegateRequest {
    private String accountId;
    private String name;
    private String description;

    protected void validate() {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Name is required");
    }
}
