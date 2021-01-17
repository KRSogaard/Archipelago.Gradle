package build.archipelago.buildserver.models.rest;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
