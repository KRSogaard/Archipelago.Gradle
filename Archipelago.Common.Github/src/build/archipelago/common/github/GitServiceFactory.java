package build.archipelago.common.github;

import build.archipelago.common.github.github.OrgGitService;
import build.archipelago.common.github.github.UserGitService;

public class GitServiceFactory {
    public GitService getGitService(String accountType, String username, String accessToken) {
        switch (accountType) {
            case GitType.GITHUB_USER:
                return new UserGitService(username, accessToken);
            case GitType.GITHUB_ORG:
                return new OrgGitService(username, accessToken);
            default:
                throw new RuntimeException("Unknown github account type");
        }
    }
}
