package build.archipelago.packageservice.models;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class GetPackageBuildRestResponse {
    private String hash;
    private long created;
    private String config;
    private String gitCommit;
    private String gitBranch;
}
