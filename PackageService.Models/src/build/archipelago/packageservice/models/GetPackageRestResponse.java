package build.archipelago.packageservice.models;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class GetPackageRestResponse {
    private String name;
    private String description;
    private long created;
    private List<VersionRestResponse> versions;

    public static class VersionRestResponse {
        private String version;
        private String latestBuildHash;
        private long latestBuildTime;

        public VersionRestResponse(String version, String latestBuildHash, long latestBuildTime) {
            this.version = version;
            this.latestBuildHash = latestBuildHash;
            this.latestBuildTime = latestBuildTime;
        }

        public String getVersion() {
            return version;
        }

        public String getLatestBuildHash() {
            return latestBuildHash;
        }

        public long getLatestBuildTime() {
            return latestBuildTime;
        }
    }
}
