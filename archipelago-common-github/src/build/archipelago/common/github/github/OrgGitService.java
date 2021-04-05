package build.archipelago.common.github.github;

import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.git.models.GitRepo;
import build.archipelago.common.git.models.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;

import java.net.http.*;

// https://docs.github.com/en/free-pro-team@latest/rest/reference/permissions-required-for-github-apps
// https://docs.github.com/en/rest/overview/endpoints-available-for-github-apps
@Slf4j
public class OrgGitService extends BaseGithubService {

    public OrgGitService(String username, String accessToken) {
        super(username, accessToken);
    }

    @Override
    public GitRepo createRepo(String name, String description, boolean privateRepo) throws UnauthorizedException, GitRepoExistsException {
        HttpResponse<String> httpResponse;
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", name);
            jsonObject.put("description", description);
            jsonObject.put("private", privateRepo);

            HttpRequest httpRequest = this.getGithubRequest("/orgs/" + username + "/repos")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonObject.toJSONString()))
                    .build();

            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        switch (httpResponse.statusCode()) {
            case 200:
            case 201:
                return this.parseToGitRepo(httpResponse.body());
            case 422:
                throw new GitRepoExistsException(name);
            case 401:
            case 403:
                throw new UnauthorizedException();
            default:
                throw new RuntimeException("Got unknown git response: " + httpResponse.statusCode());
        }
    }
}
