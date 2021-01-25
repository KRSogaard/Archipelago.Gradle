package build.archipelago.versionsetservice.models;

import build.archipelago.common.ArchipelagoPackage;
import com.google.common.base.*;
import lombok.*;

import java.util.Optional;
import java.util.*;

@Value
@Builder
public class CreateVersionSetRequest {
    private String name;
    private List<ArchipelagoPackage> targets;
    private Optional<String> parent;

    public void validate() throws IllegalArgumentException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Name is required");
        Preconditions.checkNotNull(targets, "At least 1 target is required");
        Preconditions.checkArgument(targets.size() > 0, "At least 1 target is required");
    }
}
