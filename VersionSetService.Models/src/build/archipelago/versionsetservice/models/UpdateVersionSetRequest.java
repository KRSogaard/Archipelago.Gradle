package build.archipelago.versionsetservice.models;

import build.archipelago.common.ArchipelagoPackage;
import lombok.*;

import java.util.*;

@Value
@Builder
public class UpdateVersionSetRequest {
    private List<ArchipelagoPackage> targets;
    private Optional<String> parent;

    public void validate() throws IllegalArgumentException {
        // TODO:
    }
}
