package build.archipelago.account.common.models;

import lombok.*;

@Builder
@Data
public class GitDetails {
    private String codeSource;
    private String gitHubAccessToken;
    private String githubAccount;
}
