package build.archipelago.common.github.github;

import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.github.GitService;
import build.archipelago.common.github.exceptions.GitRepoExistsException;
import build.archipelago.common.github.exceptions.RepoNotFoundException;
import build.archipelago.common.github.models.GitRepo;
import com.google.common.base.Function;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

// https://docs.github.com/en/free-pro-team@latest/rest/reference/permissions-required-for-github-apps
public class OrgGitService implements GitService {

    private static final JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
    protected HttpClient client;
    private static final String baseUrl = "https://api.github.com";
    private String username;
    private String accessToken;

    public OrgGitService(String username, String accessToken) {
        this.username = username;
        this.accessToken = accessToken;

        client = HttpClient
                .newBuilder()
                .build();
    }

    @Override
    public boolean verifyAccess() {
        // TODO: Find a vay to verify
        return true;
    }

    //@Override
    public String getRepos() throws RepoNotFoundException, UnauthorizedException {
        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = getGithubRequest("/orgs/" + username + "/repos")
                    .GET()
                    .build();

            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return validateReponse(httpResponse, body -> body);
    }

    @Override
    public boolean hasRep(String name) throws UnauthorizedException {
        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = getGithubRequest("/repos/" + username + "/" + name)
                    .GET()
                    .build();

            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        switch (httpResponse.statusCode()) {
            case 200:
                return true;
            case 404:
                return false;
            case 401:
            case 403:
                throw new UnauthorizedException();
            default:
                throw new RuntimeException("Got unknown git response");
        }
    }

    @Override
    public GitRepo getRepo(String name) throws RepoNotFoundException, UnauthorizedException {
        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = getGithubRequest("/repos/" + username + "/" + name)
                    .GET()
                    .build();

            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        switch (httpResponse.statusCode()) {
            case 200:
                return parseToGitRepo(httpResponse.body());
            case 404:
                throw new RepoNotFoundException();
            case 401:
            case 403:
                throw new UnauthorizedException();
            default:
                throw new RuntimeException("Got unknown git response");
        }
    }

    @Override
    public GitRepo createRepo(String name, String description, boolean privateRepo) throws UnauthorizedException, GitRepoExistsException {
        HttpResponse<String> httpResponse;
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", name);
            jsonObject.put("description", description);
            jsonObject.put("private", privateRepo);

            HttpRequest httpRequest = getGithubRequest("/orgs/" + username + "/repos")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonObject.toJSONString()))
                    .build();

            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        switch (httpResponse.statusCode()) {
            case 200:
            case 201:
                return parseToGitRepo(httpResponse.body());
            case 422:
                throw new GitRepoExistsException(name);
            case 401:
            case 403:
                throw new UnauthorizedException();
            default:
                throw new RuntimeException("Got unknown git response: " + httpResponse.statusCode());
        }
    }

    private GitRepo parseToGitRepo(String body) {
        JSONObject json;
        try {
            json = (JSONObject) parser.parse(body);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return GitRepo.builder()
                .name(json.getAsString("name"))
                .url(json.getAsString("html_url"))
                .cloneUrl(json.getAsString("clone_url"))
                .build();
    }

    private <T> T validateReponse(HttpResponse<String> httpResponse, Function<String, T> onOk)
            throws RepoNotFoundException, UnauthorizedException {
        switch (httpResponse.statusCode()) {
            case 401:
                throw new UnauthorizedException();
            case 404:
                throw new RepoNotFoundException();
            case 200: // Ok
                return onOk.apply(httpResponse.body());
            default:
                throw new RuntimeException("Unknown response " + httpResponse.statusCode());
        }
    }

    private HttpRequest.Builder getGithubRequest(String url) throws URISyntaxException {
        String auth = username + ":" + accessToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        if (!encodedAuth.equals("d2V3ZWxvLWNvbTpmZjljM2M5NmU3NjU5NWFkZjA3MWViNTAyOWEwNDRlZjYyZWI4YTkx")) {
            System.out.println("WRONG!");
        }
        return HttpRequest.newBuilder(new URI(baseUrl + url))
                .header("Authorization", "Basic " + encodedAuth)
                .header("accept", "application/vnd.github.v3+json");
    }
}