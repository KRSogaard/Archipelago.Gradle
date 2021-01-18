package build.archipelago.buildserver.models.rest;

import com.google.common.base.*;
import lombok.*;

import java.util.List;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class NewBuildRestRequest {
    private String versionSet;
    private boolean dryRun;
    private List<BuildPackageRestRequest> buildPackages;

    public void validate() {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSet));
        Preconditions.checkArgument(buildPackages != null && buildPackages.size() > 0);
    }
}
