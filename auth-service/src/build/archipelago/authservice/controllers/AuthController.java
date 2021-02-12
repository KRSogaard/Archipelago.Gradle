package build.archipelago.authservice.controllers;

import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.authservice.models.rest.*;
import build.archipelago.authservice.services.auth.*;
import build.archipelago.authservice.services.users.*;
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

    @PostMapping("/device")
    public void device(ActivateDeviceRestRequest request) throws TokenNotFoundException, TokenExpiredException {
        String userCode = request.getUserCode().toUpperCase();

        authService.getDeviceCode(userCode);
        authService.updateDeviceCode(userCode, request.getUserId());
    }

}
