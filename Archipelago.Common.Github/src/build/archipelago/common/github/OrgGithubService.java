package build.archipelago.common.github;

public class OrgGithubService implements GithubService {

    private String username;
    private String accessToken;

    public OrgGithubService(String username, String accessToken) {
        this.username = username;
        this.accessToken = accessToken;
    }

    public boolean verifyAccess() {
        return false;
    }
}
