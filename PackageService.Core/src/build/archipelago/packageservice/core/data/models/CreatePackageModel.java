package build.archipelago.packageservice.core.data.models;

import build.archipelago.common.ArchipelagoPackage;
import com.google.common.base.*;
import lombok.*;

@Data
@Builder
public class CreatePackageModel {
    private String name;
    private String description;

    public void validate() {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(description), "A description is required");
        Preconditions.checkArgument(ArchipelagoPackage.validateName(name), "The package name \"" + name + "\" was not valid");
    }
}
