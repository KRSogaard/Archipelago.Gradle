package build.archipelago.versionsetservice.models.rest;

import build.archipelago.common.ArchipelagoPackage;
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
    }

    public UpdateVersionSetRequest toInternal() {
        return UpdateVersionSetRequest.builder()
                .parent(this.getParent())
                .target(this.getTarget().isPresent() ?
                        Optional.of(ArchipelagoPackage.parse(this.getTarget().get()))
                        : Optional.empty())
                .build();
    }

    public static UpdateVersionSetRestRequest from(UpdateVersionSetRequest request) {
        return UpdateVersionSetRestRequest.builder()
                .parent(request.getParent())
                .target(request.getTarget().isPresent() ?
                        Optional.of(request.getTarget().get().getNameVersion())
                        : Optional.empty())
                .build();
    }
}