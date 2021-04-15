package build.archipelago.versionsetservice.models.rest;

import build.archipelago.common.versionset.VersionSetCallback;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class VersionSetCallbacksRestResponse {
    private List<VersionSetCallbackRestResponse> callbacks;

    public static VersionSetCallbacksRestResponse from(List<VersionSetCallback> callbacks) {
        return VersionSetCallbacksRestResponse.builder()
                .callbacks(callbacks.stream().map(VersionSetCallbackRestResponse::form).collect(Collectors.toList()))
                .build();
    }
}
