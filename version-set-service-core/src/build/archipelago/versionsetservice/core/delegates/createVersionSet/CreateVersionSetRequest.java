package build.archipelago.versionsetservice.core.delegates.createVersionSet;

import build.archipelago.common.ArchipelagoPackage;
import lombok.Builder;
import lombok.Value;

import java.util.Optional;

@Builder
@Value
public class CreateVersionSetRequest {
    private String accountId;
    private String name;
    private ArchipelagoPackage target;
    private String parent;
}
