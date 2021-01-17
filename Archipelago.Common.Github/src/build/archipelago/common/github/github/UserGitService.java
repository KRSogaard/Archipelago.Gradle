package build.archipelago.common.github.github;

import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.github.GitService;
import build.archipelago.common.github.exceptions.GitRepoExistsException;
import build.archipelago.common.github.exceptions.RepoNotFoundException;
import build.archipelago.common.github.models.GitRepo;

import java.nio.file.Path;

public class UserGitService implements GitService {

    private String username;
    private String accessToken;

    public UserGitService(String username, String accessToken) {
        this.username = username;
        this.accessToken = accessToken;
    }

    @Override
    public boolean verifyAccess() {
        return false;
    }

    @Override
    public boolean hasRep(String name) throws UnauthorizedException {
        return false;
    }

    @Override
    public GitRepo getRepo(String name) throws RepoNotFoundException, UnauthorizedException {
        return null;
    }

    @Override
    public GitRepo createRepo(String name, String description, boolean privateRepo) throws UnauthorizedException, GitRepoExistsException {
        return null;
    }

    @Override
    public void downloadRepoZip(Path filePath, String gitRepoFullName, String commit) throws RepoNotFoundException {
    }
}
