package build.archipelago.common.github.github;

import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.git.models.GitBranch;
import build.archipelago.common.git.models.GitCommit;
import build.archipelago.common.git.models.GitRepo;
import build.archipelago.common.github.GitService;
import build.archipelago.common.git.models.exceptions.BranchNotFoundException;
import build.archipelago.common.git.models.exceptions.GitRepoExistsException;
import build.archipelago.common.git.models.exceptions.RepoNotFoundException;
import build.archipelago.common.github.models.github.GithubBranch;
import build.archipelago.common.github.models.github.GithubCommit;
import build.archipelago.common.github.utils.CommitUtils;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class BaseGithubService implements GitService {

    protected static final JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
    protected HttpClient client;
    private static final String baseUrl = "https://api.github.com";
    protected String username;
    protected String accessToken;

    public BaseGithubService(String username, String accessToken) {
        this.username = username;
        this.accessToken = accessToken;

        client = HttpClient
                .newBuilder()
                .build();
    }

    @Override
    public boolean verifyAccess() {
        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = this.getGithubRequest("/user")
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
            case 401:
            case 403:
            default:
                return false;
        }
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

            HttpRequest httpRequest = this.getGithubRequest("/user/repos")
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
            log.debug("Fetching git zip file from '{}'", url);
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
                        log.debug("Git zip got redirected to '{}'", location.get());
                        HttpRequest httpRequest = this.getGithubRequest(location.get(), false)
                                .GET().build();
                        response = client.send(httpRequest, HttpResponse.BodyHandlers.ofFile(filePath));
                        if (response.statusCode() != 200) {
                            throw new RuntimeException("Was unable to get the redirected file from github got status code " + response.statusCode() + ". url: " + location.get());
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

    @Override
    public List<GitBranch> getBranches(String gitRepoFullName) throws RepoNotFoundException {
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

    @Override
    public List<GitCommit> getCommits(String gitRepoFullName, String branch) throws RepoNotFoundException, BranchNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(gitRepoFullName));

        List<GitBranch> branches = getBranches(gitRepoFullName);
        Optional<GitBranch> selectedBranch = branches.stream().filter(b -> branch.equalsIgnoreCase(b.getName())).findFirst();
        if (selectedBranch.isEmpty()) {
            throw new BranchNotFoundException(branch);
        }

        HttpResponse<String> httpResponse;
        try {
            HttpRequest httpRequest = this.getGithubRequest("/repos/" + gitRepoFullName + "/commits")
                    .GET().build();
            httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<GithubCommit> allCommits;
        switch (httpResponse.statusCode()) {
            case 200:
            case 201:
                allCommits = CommitUtils.walkCommits(this.parseCommits(httpResponse.body()), selectedBranch.get().getSha());
                break;
            case 401:
            case 403:
                throw new UnauthorizedException();
            case 404:
                throw new RepoNotFoundException(gitRepoFullName);
            default:
                throw new RuntimeException("Got unknown git response: " + httpResponse.statusCode());
        }

        // It is minus to reverse
        return allCommits.stream().map(GithubCommit::toInternal)
                .sorted(Comparator.comparingLong(a -> -a.getCreated().getEpochSecond()))
                .collect(Collectors.toList());
    }

    private List<GitBranch> parseBranches(String body) {
        Gson gson = new Gson();
        List<GithubBranch> list = gson.fromJson(body, new TypeToken<List<GithubBranch>>() {}.getType());
        return list.stream().map(b -> GitBranch.builder()
                .sha(b.getCommit().getSha())
                .name(b.getName())
                .build()).collect(Collectors.toList());
    }

    private List<GithubCommit> parseCommits(String body) {
        Gson gson = new Gson();
        return gson.fromJson(body, new TypeToken<List<GithubCommit>>() {}.getType());
    }

    protected GitRepo parseToGitRepo(String body) {
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

    protected HttpRequest.Builder getGithubRequest(String url) throws URISyntaxException {
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
