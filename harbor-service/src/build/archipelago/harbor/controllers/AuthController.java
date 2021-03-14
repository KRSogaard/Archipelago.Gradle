package build.archipelago.harbor.controllers;

import build.archipelago.authservice.client.*;
import build.archipelago.authservice.models.client.*;
import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.authservice.models.rest.*;
import build.archipelago.harbor.models.auth.ActivateDeviceRestRequest;
import build.archipelago.common.exceptions.*;
import build.archipelago.harbor.filters.*;
import com.google.common.base.*;
import lombok.extern.slf4j.*;
import org.springframework.web.bind.annotation.*;
import build.archipelago.harbor.models.auth.LogInRestRequest;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
@Slf4j
public class AuthController {
    private AuthClient authClient;

    public AuthController(AuthClient authClient) {
        this.authClient = authClient;
    }

    @PostMapping("/register")
    public void register(@RequestBody RegisterAccountRestRequest model) throws UserExistsException {
        Preconditions.checkNotNull(model);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(model.getName()), "Name is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(model.getEmail()), "Email is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(model.getPassword()), "Password is required");

        log.info("Getting request to register '{}'", model.getEmail());
        authClient.register(RegisterRequest.builder()
                .name(model.getName())
                .email(model.getEmail())
                .password(model.getPassword())
                .build());
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

    @PostMapping("/device")
    public void deviceActivation(
            @RequestAttribute(AccountIdFilter.UserIdKey) String userId,
            @RequestBody ActivateDeviceRestRequest request) throws TokenExpiredException, TokenNotFoundException {
        if (Strings.isNullOrEmpty(userId)) {
            throw new UnauthorizedException();
        }
        Preconditions.checkArgument(!Strings.isNullOrEmpty(request.getUserCode()));
        authClient.device(ActivateDeviceRequest.builder()
                .userCode(request.getUserCode())
                .userId(userId)
        .build());
    }
}
