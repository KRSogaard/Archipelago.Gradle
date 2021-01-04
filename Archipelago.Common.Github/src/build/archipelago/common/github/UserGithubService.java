package build.archipelago.common.github;

public class UserGithubService implements GithubService {

    private String username;
    private String accessToken;

    public UserGithubService(String username, String accessToken) {
        this.username = username;
        this.accessToken = accessToken;
    }

    public boolean verifyAccess() {
        return false;
    }
}
