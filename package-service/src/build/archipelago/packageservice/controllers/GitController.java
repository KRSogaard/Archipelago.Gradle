package build.archipelago.packageservice.controllers;

import build.archipelago.common.git.models.GitBranch;
import build.archipelago.common.git.models.exceptions.BranchNotFoundException;
import build.archipelago.common.git.models.exceptions.GitDetailsNotFound;
import build.archipelago.common.git.models.exceptions.RepoNotFoundException;
import build.archipelago.packageservice.core.delegates.getPackageBranches.GetPackageBranchesDelegate;
import build.archipelago.packageservice.core.delegates.getPackageCommits.GetPackageCommitsDelegate;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.models.GitBranchesResponse;
import build.archipelago.packageservice.models.GitCommitsResponse;
import build.archipelago.packageservice.models.rest.GitBranchesRestResponse;
import build.archipelago.packageservice.models.rest.GitCommitRestResponse;
import build.archipelago.packageservice.models.rest.GitCommitsRestResponse;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("account/{accountId}/git")
@Slf4j
@CrossOrigin(origins = "*")
public class GitController {

    private GetPackageBranchesDelegate packageBranchesDelegate;
    private GetPackageCommitsDelegate packageCommitsDelegate;

    public GitController(GetPackageBranchesDelegate packageBranchesDelegate, GetPackageCommitsDelegate packageCommitsDelegate) {
        Preconditions.checkNotNull(packageBranchesDelegate);
        this.packageBranchesDelegate = packageBranchesDelegate;
        this.packageCommitsDelegate = packageCommitsDelegate;
    }

    @GetMapping(value = "{name}/branches")
    @ResponseStatus(HttpStatus.OK)
    public GitBranchesRestResponse getPackageBranches(
            @PathVariable("accountId") String accountId,
            @PathVariable("name") String name
    ) throws PackageNotFoundException, GitDetailsNotFound, RepoNotFoundException {
        log.info("Request to get git branches for package {} on account {}", name, accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));

        GitBranchesResponse branches = packageBranchesDelegate.get(accountId, name);
        return GitBranchesRestResponse.builder()
                .branches(branches.getBranches())
                .build();
    }

    @GetMapping(value = "{name}/{branch}")
    @ResponseStatus(HttpStatus.OK)
    public GitCommitsRestResponse getPackageCommits(
            @PathVariable("accountId") String accountId,
            @PathVariable("name") String name,
            @PathVariable("branch") String branch
    ) throws PackageNotFoundException, GitDetailsNotFound, RepoNotFoundException, BranchNotFoundException {
        log.info("Request to get git branches for package {} on account {}", name, accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));

        GitCommitsResponse commits = packageCommitsDelegate.get(accountId, name, branch);
        return GitCommitsRestResponse.builder()
                .commits(commits.getCommits().stream().map(GitCommitRestResponse::from).collect(Collectors.toList()))
                .build();
    }
}
