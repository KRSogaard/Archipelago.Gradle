package build.archipelago.versionsetservice.models.rest;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.utils.O;
import build.archipelago.versionsetservice.models.UpdateVersionSetRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Optional;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateVersionSetRestRequest {
    private Optional<String> target;
    private Optional<String> parent;

    public void validate() throws IllegalArgumentException {
        if (O.isPresent(target)) {
            ArchipelagoPackage.parse(target.get());
        }
    }

    public UpdateVersionSetRequest toInternal() {
        return UpdateVersionSetRequest.builder()
                .parent(this.getParent())
                .target(O.getOrDefault(this.getTarget(), Optional.empty(), t -> Optional.of(ArchipelagoPackage.parse(t))))
                .build();
    }

    public static UpdateVersionSetRestRequest from(UpdateVersionSetRequest request) {
        return UpdateVersionSetRestRequest.builder()
                .parent(request.getParent())
                .target(O.getOrDefault(request.getTarget(), Optional.empty(), t -> Optional.of(t.getNameVersion())))
                .build();
    }
}