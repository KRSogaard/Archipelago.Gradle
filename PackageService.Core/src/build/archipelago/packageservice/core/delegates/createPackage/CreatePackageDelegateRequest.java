package build.archipelago.packageservice.core.delegates.createPackage;

import com.google.common.base.Preconditions;
import lombok.*;

@Data
@Builder
public class CreatePackageDelegateRequest {
    private String name;
    private String description;

    protected void validate() {
        Preconditions.checkNotNull(name, "Name required");
        Preconditions.checkNotNull(description, "Description required");
    }
}
