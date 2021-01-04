package build.archipelago.packageservice.core.delegates.createPackage;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreatePackageDelegateRequest {
    private String accountId;
    private String name;
    private String description;

    protected void validate() {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(description), "Description is required");
    }
}
