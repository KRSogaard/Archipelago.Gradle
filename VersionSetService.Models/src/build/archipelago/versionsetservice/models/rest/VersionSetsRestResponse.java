package build.archipelago.versionsetservice.models.rest;

import lombok.*;

import java.util.List;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class VersionSetsRestResponse {
    private List<VersionSetRestResponse> versionSets;
}
