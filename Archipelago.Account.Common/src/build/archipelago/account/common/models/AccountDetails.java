package build.archipelago.account.common.models;

import lombok.*;

@Builder
@Data
public class AccountDetails {
    private String id;
    private String name;
    private String codeSource;
    private String gitHubApi;
    private String gitHubClientId;
    private String gitHubClientSecret;
    private String githubCode;
    private String gitHubRepo;
}
