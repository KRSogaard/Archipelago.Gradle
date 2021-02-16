package build.archipelago.harbor.controllers;

import build.archipelago.account.common.AccountService;
import build.archipelago.account.common.models.GitDetails;
import build.archipelago.common.github.*;
import build.archipelago.harbor.filters.AccountIdFilter;
import build.archipelago.harbor.models.git.GitAccessRequest;
import com.google.common.base.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("git")
@Slf4j
@CrossOrigin(origins = "*")
public class GitController {

    @Autowired
    public AccountService accountService;
    @Autowired
    public GitServiceFactory gitServiceFactory;

    @PostMapping(value = "/verify", consumes = "application/json")
    public ResponseEntity verifyGithub(@RequestBody GitAccessRequest request) {
        GitService git = gitServiceFactory.getGitService(request.getType(), request.getUsername(), request.getAccessToken());

        if (!git.verifyAccess()) {
            return ResponseEntity.status(HttpStatus.SC_NOT_ACCEPTABLE).build();
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping(consumes = "application/json")
    public void saveGithub(@RequestAttribute(AccountIdFilter.AccountIdKey) String accountId,
                                     @RequestBody GitAccessRequest request) {
        log.info("Updating git details for account: {}", accountId);
        Preconditions.checkArgument(request != null, "Git details required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getType()), "Account type required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getUsername()), "Username required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getAccessToken()), "Access token required");

        GitService git = gitServiceFactory.getGitService(request.getType(), request.getUsername(), request.getAccessToken());
        Preconditions.checkArgument(git.verifyAccess(), "Git access was invalid");

        accountService.saveGit(accountId, GitDetails.builder()
                .codeSource(request.getType())
                .githubAccount(request.getUsername())
                .gitHubAccessToken(request.getAccessToken())
                .build());
    }
}
