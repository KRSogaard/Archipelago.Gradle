package build.archipelago.common.github.github;

import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.github.GitService;
import build.archipelago.common.github.exceptions.*;
import build.archipelago.common.github.models.GitBranch;
import build.archipelago.common.github.models.GitRepo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.*;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

// https://docs.github.com/en/free-pro-team@latest/rest/reference/permissions-required-for-github-apps
// https://docs.github.com/en/rest/overview/endpoints-available-for-github-apps
@Slf4j
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

    @Override
    public boolean hasRep(String name) throws UnauthorizedException {
        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = this.getGithubRequest("/repos/" + username + "/" + name)
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
            HttpRequest httpRequest = this.getGithubRequest("/repos/" + username + "/" + name)
                    .GET()
                    .build();

            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        switch (httpResponse.statusCode()) {
            case 200:
                return this.parseToGitRepo(httpResponse.body());
            case 404:
                throw new RepoNotFoundException(name);
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

    @Override
    public void downloadRepoZip(Path filePath, String gitRepoFullName, String commit) throws RepoNotFoundException {
        HttpResponse<Path> response;
        try {
            String url = String.format("https://github.com/%s/archive/%s.zip", gitRepoFullName, commit);

            HttpRequest httpRequest = this.getGithubRequest(url, false)
                    .GET().build();
            response = client.send(httpRequest, HttpResponse.BodyHandlers.ofFile(filePath));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        switch (response.statusCode()) {
            case 200:
                break;
            case 302:
                Optional<String> location = response.headers().firstValue("location");
                if (location.isPresent()) {
                    try {
                        HttpRequest httpRequest = this.getGithubRequest(location.get(), false)
                                .GET().build();
                        response = client.send(httpRequest, HttpResponse.BodyHandlers.ofFile(filePath));
                        if (response.statusCode() != 200) {
                            throw new RuntimeException("Was unable to get the redirected file from github: " + location.get());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw new RuntimeException("Returned 302 but no location was given");
                }
                break;
            case 401:
            case 403:
                throw new UnauthorizedException();
            case 404:
                throw new RepoNotFoundException(gitRepoFullName);
        }
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Failed to download the zip file for package: " + gitRepoFullName);
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
                .fullName(json.getAsString("full_name"))
                .privateRepo("true".equalsIgnoreCase(json.getAsString("private")))
                .build();
    }

    @Override
    public List<String> getBranches(String gitRepoFullName) throws RepoNotFoundException {
        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = this.getGithubRequest("/repos/" + gitRepoFullName + "/branches")
                    .GET().build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        switch (httpResponse.statusCode()) {
            case 200:
            case 201:
                return this.parseBranches(httpResponse.body());
            case 401:
            case 403:
                throw new UnauthorizedException();
            case 404:
                throw new RepoNotFoundException(gitRepoFullName);
            default:
                throw new RuntimeException("Got unknown git response: " + httpResponse.statusCode());
        }
    }

    private List<String> parseBranches(String body) {
        Gson gson = new Gson();
        List<GitBranch> list = gson.fromJson(body, new TypeToken<List<GitBranch>>() {}.getType());
        return list.stream().map(GitBranch::getName).collect(Collectors.toList());
    }

    private HttpRequest.Builder getGithubRequest(String url) throws URISyntaxException {
        return this.getGithubRequest(url, true);
    }

    private HttpRequest.Builder getGithubRequest(String url, boolean prepend) throws URISyntaxException {
        String auth = username + ":" + accessToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String finalUrl = prepend ? baseUrl + url : url;
        log.debug("Creating github request to: " + finalUrl);
        return HttpRequest.newBuilder(new URI(finalUrl))
                .header("Authorization", "Basic " + encodedAuth)
                .header("accept", "application/vnd.github.v3+json");
    }
}
