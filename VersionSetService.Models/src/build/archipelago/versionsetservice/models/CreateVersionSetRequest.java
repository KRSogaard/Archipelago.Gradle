package build.archipelago.versionsetservice.models;

import build.archipelago.common.ArchipelagoPackage;
import com.google.common.base.*;
import lombok.*;

import java.util.Optional;

@Value
@Builder
public class CreateVersionSetRequest {
    private String name;
    private Optional<ArchipelagoPackage> target;
    private Optional<String> parent;

    public void validate() throws IllegalArgumentException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Name is required");
    }
}
