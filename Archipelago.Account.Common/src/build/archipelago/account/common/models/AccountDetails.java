package build.archipelago.account.common.models;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AccountDetails {
    private String id;
    private String codeSource;
    private String gitHubAccessToken;
    private String githubAccount;
}
