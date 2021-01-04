package build.archipelago.common.github;

public class GithubServiceFactory {
    public static GithubService GithubService(String accountType, String username, String accessToken) {
        switch (accountType) {
            case GithubType.GITHUB_USER:
                return new UserGithubService(username, accessToken);
            case GithubType.GITHUB_ORG:
                return new OrgGithubService(username, accessToken);
            default:
                throw new RuntimeException("Unknown github account type");
        }
    }
}
