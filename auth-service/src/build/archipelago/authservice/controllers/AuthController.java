package build.archipelago.authservice.controllers;

import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.authservice.models.rest.*;
import build.archipelago.authservice.services.auth.AuthService;
import build.archipelago.authservice.services.auth.models.*;
import build.archipelago.authservice.services.users.UserService;
import com.google.common.base.*;
import lombok.extern.slf4j.*;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/cookie")
    public CreateAuthCookieResult createAuthCookie(@RequestBody CreateAuthCookieRequest model) {
        CodeResponse cookieCode = authService.createAuthCookie(model.getUserId());

        return CreateAuthCookieResult.builder()
                .cookieCode(cookieCode.getCode())
                .expires(cookieCode.getExpires().getEpochSecond())
                .build();
    }

    @GetMapping("/cookie/{authCookie}")
    public GetUserFromAuthCookieResult getUserFromAuthCookie(@PathVariable("authCookie") String authCookie)
            throws UserNotFoundException, TokenNotFoundException, TokenExpiredException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(authCookie), "authCookie is required");

        String userId = authService.getUserFromAuthCookie(authCookie);

        return GetUserFromAuthCookieResult.builder()
                .userId(userId)
                .build();
    }

    @PostMapping("/device")
    public void device(ActivateDeviceRestRequest request) throws TokenNotFoundException, TokenExpiredException {
        String userCode = request.getUserCode().toUpperCase();

        authService.getDeviceCode(userCode);
        authService.updateDeviceCode(userCode, request.getUserId());
    }

}
