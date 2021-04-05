package build.archipelago.common.github;

import build.archipelago.common.exceptions.UnauthorizedException;
import build.archipelago.common.git.models.GitBranch;
import build.archipelago.common.git.models.GitCommit;
import build.archipelago.common.git.models.GitRepo;
import build.archipelago.common.git.models.exceptions.*;

import java.nio.file.Path;
import java.util.List;

public interface GitService {
    boolean verifyAccess();

    boolean hasRep(String name) throws UnauthorizedException;

    GitRepo getRepo(String name) throws RepoNotFoundException, UnauthorizedException;

    GitRepo createRepo(String name, String description, boolean privateRepo) throws UnauthorizedException, GitRepoExistsException;

    void downloadRepoZip(Path filePath, String gitRepoFullName, String commit) throws RepoNotFoundException;

    List<GitBranch> getBranches(String name) throws RepoNotFoundException;

    List<GitCommit> getCommits(String name, String branch) throws RepoNotFoundException, BranchNotFoundException;
}
