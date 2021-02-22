package build.archipelago.packageservice.models;

import build.archipelago.common.ArchipelagoPackage;
import com.google.common.base.*;
import lombok.*;

@Value
@Builder
public class CreatePackageModel {
    private String name;
    private String description;
    private String gitCloneUrl;
    private String gitUrl;
    private String gitRepoName;
    private String gitFullName;

    public void validate() {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Name is required");
        Preconditions.checkArgument(ArchipelagoPackage.validateName(name), "The package name \"" + name + "\" was not valid");
    }
}
