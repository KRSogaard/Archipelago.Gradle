package build.archipelago.packageservice.controllers;

import build.archipelago.account.common.exceptions.GitDetailsNotFound;
import build.archipelago.common.github.exceptions.RepoNotFoundException;
import build.archipelago.packageservice.core.delegates.getPackageBranches.GetPackageBranchesDelegate;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.models.GitRepoBranchesResponse;
import build.archipelago.packageservice.models.rest.GetPackageRestResponse;
import build.archipelago.packageservice.models.rest.GitRepoBranchesRestResponse;
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

    public GitController(GetPackageBranchesDelegate packageBranchesDelegate) {
        Preconditions.checkNotNull(packageBranchesDelegate);
        this.packageBranchesDelegate = packageBranchesDelegate;
    }

    @GetMapping(value = "{name}/branches")
    @ResponseStatus(HttpStatus.OK)
    public GitRepoBranchesRestResponse getPackageBranches(
            @PathVariable("accountId") String accountId,
            @PathVariable("name") String name
    ) throws PackageNotFoundException, GitDetailsNotFound, RepoNotFoundException {
        log.info("Request to get git branches for package {} on account {}", name, accountId);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));

        GitRepoBranchesResponse branches = packageBranchesDelegate.get(accountId, name);
        return GitRepoBranchesRestResponse.builder()
                .branches(branches.getBranches())
                .build();
    }
}
