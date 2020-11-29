package build.archipelago.account.common.models;

import lombok.*;

@Builder
@Data
public class AccountDetails {
    private String id;
    private String codeSource;
    private String gitHubAccessToken;
    private String githubAccount;
}
