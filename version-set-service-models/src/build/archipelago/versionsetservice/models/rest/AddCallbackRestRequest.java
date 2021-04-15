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
public class AddCallbackRestRequest {
    private String url;
}