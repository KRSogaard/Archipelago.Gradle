package build.archipelago.authservice.controllers;

import build.archipelago.authservice.models.rest.AuthorizeRestRequest;
import build.archipelago.authservice.services.auth.AuthService;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Slf4j
@RequestMapping("api")
public class AuthApiController {

    private AuthService authService;

    public AuthApiController(AuthService authService) {
        this.authService = authService;
    }


}
