package build.archipelago.harbor.controllers;

import build.archipelago.authservice.client.*;
import build.archipelago.authservice.models.client.*;
import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.authservice.models.rest.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.harbor.filters.*;
import build.archipelago.harbor.models.auth.LogInRestRequest;
import com.google.common.base.*;
import lombok.extern.slf4j.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
@Slf4j
public class AuthController {
    private AuthClient authClient;

    public AuthController(AuthClient authClient) {
        this.authClient = authClient;
    }

    @PostMapping("/login")
    public LogInRestResponse login(@RequestBody LogInRestRequest model) throws UnauthorizedException {
        log.info("Getting request to log in '{}'", model.getEmail());
        try {
            LogInResponse response = authClient.login(model.toInternal());
            return LogInRestResponse.builder()
                    .authToken(response.getAuthToken())
                    .build();
        } catch (UserNotFoundException e) {
            log.info("The user '{}' was not found", e.getEmail());
            // We don't want to tell the caller if the user exists or not
            throw new UnauthorizedException();
        }
    }

    @PostMapping("/authToken")
    public LogInRestResponse authToken(
            @RequestAttribute(AccountIdFilter.UserIdKey) String userId,
            @RequestBody CreateAuthTokenRestRequest model) throws UnauthorizedException {
        if (Strings.isNullOrEmpty(userId)) {
            throw new UnauthorizedException();
        }
        log.info("Getting request to create auth token for '{}'", userId);
        try {
            LogInResponse response = authClient.createAuthToken(userId, model.toInternal());
            return LogInRestResponse.builder()
                    .authToken(response.getAuthToken())
                    .build();
        } catch (Exception e) {
            log.info(String.format("Got exception when creating auth token for '%s'", userId), e);
            // We don't want to tell the caller if the user exists or not
            throw new UnauthorizedException();
        }
    }
}
