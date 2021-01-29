package build.archipelago.common.github;

import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.github.exceptions.*;
import build.archipelago.common.github.models.GitRepo;

import java.nio.file.Path;

public interface GitService {
    boolean verifyAccess();

    //String getRepos() throws NotFoundException, UnauthorizedException;
    boolean hasRep(String name) throws UnauthorizedException;

    GitRepo getRepo(String name) throws RepoNotFoundException, UnauthorizedException;

    GitRepo createRepo(String name, String description, boolean privateRepo) throws UnauthorizedException, GitRepoExistsException;

    void downloadRepoZip(Path filePath, String gitRepoFullName, String commit) throws RepoNotFoundException;
}
