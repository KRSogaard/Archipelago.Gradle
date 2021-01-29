package build.archipelago.versionsetservice.models;

import build.archipelago.common.ArchipelagoPackage;
import lombok.*;

import java.util.Optional;

@Value
@Builder
public class UpdateVersionSetRequest {
    private Optional<ArchipelagoPackage> target;
    private Optional<String> parent;

    public void validate() throws IllegalArgumentException {
    }
}
