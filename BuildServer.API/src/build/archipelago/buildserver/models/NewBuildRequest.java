package build.archipelago.buildserver.models;

import lombok.*;

import java.util.List;

@Builder
@Value
public class NewBuildRequest {
    private final String versionSet;
    private final boolean dryRun;
    private final List<BuildPackageRequest> buildPackages;

    public static class BuildPackageRequest {
        private final String packageName;
        private final String branch;
        private final String commit;

        public BuildPackageRequest(String packageName, String branch, String commit) {
            this.packageName = packageName;
            this.branch = branch;
            this.commit = commit;
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
