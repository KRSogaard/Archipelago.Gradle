package build.archipelago.common.github;

import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.github.exceptions.GitRepoExistsException;
import build.archipelago.common.github.exceptions.NotFoundException;
import build.archipelago.common.github.models.GitRepo;

public interface GitService {
    boolean verifyAccess();
    //String getRepos() throws NotFoundException, UnauthorizedException;
    boolean hasRep(String name) throws UnauthorizedException;
    GitRepo getRepo(String name) throws NotFoundException, UnauthorizedException;
    GitRepo createRepo(String name, String description, boolean privateRepo) throws UnauthorizedException, GitRepoExistsException;
}
