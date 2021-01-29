package build.archipelago.buildserver.models.rest;

import lombok.*;

import java.util.List;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class BuildPackagesRestResponse {
    private List<BuildPackageStatusRestResponse> packages;
}
