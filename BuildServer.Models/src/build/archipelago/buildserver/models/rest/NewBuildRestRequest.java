package build.archipelago.buildserver.models.rest;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class NewBuildRestRequest {
    private String versionSet;
    private boolean dryRun;
    private List<BuildPackageRequest> buildPackages;

    public void validate() {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSet));
        Preconditions.checkArgument(buildPackages != null && buildPackages.size() > 0);
    }

    public static class BuildPackageRequest {
        private String packageName;
        private String branch;
        private String commit;

        public BuildPackageRequest(String packageName, String branch, String commit) {
            this.packageName = packageName;
            this.branch = branch;
            this.commit = commit;
        }

        private BuildPackageRequest() {
        }

        public String getPackageName() {
            return packageName;
        }

        public String getBranch() {
            return branch;
        }

        public String getCommit() {
            return commit;
        }
    }
}
