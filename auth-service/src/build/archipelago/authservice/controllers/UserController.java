package build.archipelago.authservice.controllers;

import build.archipelago.account.common.AccountService;
import build.archipelago.account.common.exceptions.AccountExistsException;
import build.archipelago.authservice.models.exceptions.UserExistsException;
import build.archipelago.authservice.models.rest.RegisterAccountRestRequest;
import build.archipelago.authservice.services.users.UserService;
import build.archipelago.authservice.services.users.models.UserModel;
import build.archipelago.authservice.utils.RandomUtil;
import build.archipelago.common.utils.Rando;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import build.archipelago.account.common.AccountService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    private UserService userService;
    private AccountService accountService;

    public UserController(UserService userService,
                          AccountService accountService) {
        this.userService = userService;
        this.accountService = accountService;
    }

    @PostMapping("/register")
    public void register(@RequestBody RegisterAccountRestRequest model) throws UserExistsException {
        Preconditions.checkNotNull(model);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(model.getName()), "Name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(model.getEmail()), "Email is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(model.getPassword()), "Password is required");

        String userId = userService.createUser(UserModel.builder()
                .name(model.getName())
                .email(model.getEmail())
                .password(model.getPassword())
                .build());

        String accountName = model.getName()
                .replaceAll("[^A-Za-z0-9-]", "-")
                .replaceAll("[-]+", "-");
        accountName = accountName.substring(0, Math.min(accountName.length(), 15));

        String base = accountName;
        boolean accountCreated = false;
        while (!accountCreated) {
            try {
                log.info("Creating account '{}'", accountName);
                accountService.createAccount(accountName);
                accountCreated = true;
            } catch (AccountExistsException e) {
                log.warn("The account id '{}' already existed", accountName);
                accountName = base + "-" + Rando.getRandomString().substring(0, 4);
            }
        }

        accountService.attachUserToAccount(accountName, userId);
    }
}
