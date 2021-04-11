package build.archipelago.authservice.controllers;

import build.archipelago.authservice.models.AccessKey;
import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.authservice.models.rest.*;
import build.archipelago.authservice.services.auth.*;
import build.archipelago.authservice.services.users.*;
import lombok.extern.slf4j.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {
    private AuthService authService;
    private UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public LogInRestResponse login(@RequestBody LogInRestRequest model) throws UserNotFoundException {
        model.validate();

        String userId = userService.authenticate(model.getEmail(), model.getPassword());
        String authToken = authService.createAuthToken(userId, model.toInternal());

        return LogInRestResponse.builder()
                .authToken(authToken)
                .build();
    }

    @PostMapping("/user/{userId}/create-token")
    public LogInRestResponse createAuthToken(
            @PathVariable("userId") String userId,
            @RequestBody CreateAuthTokenRestRequest request) {
        request.validate();

        String authToken = authService.createAuthToken(userId, request.toInternal());

        return LogInRestResponse.builder()
                .authToken(authToken)
                .build();
    }

    @PostMapping("/device")
    public void device(@RequestBody ActivateDeviceRestRequest request) throws TokenNotFoundException, TokenExpiredException {
        String userCode = request.getUserCode().toUpperCase();

        authService.getDeviceCode(userCode);
        authService.updateDeviceCode(userCode, request.getUserId());
    }

    @PostMapping("/accessKey/{accountId}")
    public AccessKeyRestResponse createAccessKey(
            @PathVariable("accountId") String accountId) {
        AccessKey key = authService.createAccessKey(accountId, "");
        return AccessKeyRestResponse.from(key);
    }

    @GetMapping("/accessKey/{accountId}")
    public AccessKeysRestResponse getAccessKeys(
            @PathVariable("accountId") String accountId) {
        List<AccessKey> keys = authService.getAccessKeys(accountId);
        return AccessKeysRestResponse.from(keys);
    }

    @PostMapping("/accessKey/{username}/{token}")
    public String verifyAccessKey(
            @PathVariable("username") String username,
            @PathVariable("token") String token) throws AccessKeyNotFound {
        AccessKey accessKey = authService.getAccessKey(username);
        if (!accessKey.getKey().equals(token)) {
            throw new AccessKeyNotFound();
        }
        return accessKey.getAccountId();
    }

    @DeleteMapping("/accessKey/{username}")
    public void deleteAccessKey(@PathVariable("username") String username) {
        authService.deleteAccessKey(username);
    }

}
