package build.archipelago.authservice.controllers;

import build.archipelago.account.common.AccountService;
import build.archipelago.authservice.models.AccessKey;
import build.archipelago.authservice.models.exceptions.AccessKeyNotFound;
import build.archipelago.authservice.models.exceptions.UserNotFoundException;
import build.archipelago.authservice.models.rest.AccessKeyRestResponse;
import build.archipelago.authservice.models.rest.AccessKeysRestResponse;
import build.archipelago.authservice.models.rest.UserAccountsRestResult;
import build.archipelago.authservice.models.rest.VerifyAccountMembershipRestRequest;
import build.archipelago.authservice.services.accessKeys.AccessKeyService;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/account")
@Slf4j
public class AccountController {

    private AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/verify-membership")
    public void verifyMembership(@RequestBody VerifyAccountMembershipRestRequest model) throws UserNotFoundException {
        Preconditions.checkNotNull(model);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(model.getAccountId()), "Account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(model.getUserId()), "User id is required");

        if (!accountService.isUserInAccount(model.getAccountId(), model.getUserId())) {
            throw new UserNotFoundException(model.getUserId());
        }
    }

    @GetMapping("/user/{userId}")
    public UserAccountsRestResult getUserAccounts(@PathVariable("userId") String userId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "User id is required");

        return UserAccountsRestResult.builder()
                .accounts(accountService.getAccountsForUser(userId))
                .build();
    }
}
