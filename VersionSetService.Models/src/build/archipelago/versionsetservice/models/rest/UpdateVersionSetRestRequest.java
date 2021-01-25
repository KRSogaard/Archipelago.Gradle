package build.archipelago.versionsetservice.models.rest;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.versionsetservice.models.UpdateVersionSetRequest;
import lombok.*;

import java.util.*;
import java.util.stream.Collectors;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class UpdateVersionSetRestRequest {
    private List<String> targets;
    private String parent;

    public void validate() throws IllegalArgumentException {
        // TODO: Do we need targets on update?
    }

    public UpdateVersionSetRequest toInternal() {
        return UpdateVersionSetRequest.builder()
                .parent(Optional.ofNullable(this.getParent()))
                .targets(this.getTargets() == null ? new ArrayList<>() : this.getTargets().stream().map(ArchipelagoPackage::parse).collect(Collectors.toList()))
                .build();
    }
}