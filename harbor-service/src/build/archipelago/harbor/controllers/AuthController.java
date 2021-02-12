package build.archipelago.harbor.controllers;

import build.archipelago.authservice.client.*;
import build.archipelago.authservice.models.client.*;
import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.authservice.models.rest.*;
import build.archipelago.harbor.models.auth.LogInRestRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private AuthClient authClient;

    public AuthController(AuthClient authClient) {
        this.authClient = authClient;
    }

    @PostMapping("/login")
    public LogInRestResponse login(@RequestBody LogInRestRequest model) throws UserNotFoundException {
        LogInResponse response = authClient.login(model.toInternal());
        return LogInRestResponse.builder()
                .authToken(response.getAuthToken())
                .build();
    }
}
