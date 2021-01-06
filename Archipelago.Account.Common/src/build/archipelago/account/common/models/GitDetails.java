package build.archipelago.account.common.models;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GitDetails {
    private String codeSource;
    private String gitHubAccessToken;
    private String githubAccount;
}
