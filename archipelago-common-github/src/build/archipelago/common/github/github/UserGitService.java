package build.archipelago.common.github.github;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserGitService extends BaseGithubService {

    public UserGitService(String username, String accessToken) {
        super(username, accessToken);
    }
}
