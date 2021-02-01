package build.archipelago.authservice.controllers;

import build.archipelago.authservice.models.*;
import build.archipelago.authservice.services.AuthService;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import javax.validation.Valid;

@Controller
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/authorize")
    public String getAuthorization(
           // @RequestHeader("authorization") String authorizationHeader,
            @RequestParam(name="response_type") String response_type,
            @RequestParam(name="response_mode", required=false) String response_mode,
            @RequestParam(name="client_id") String client_id,
            @RequestParam(name="redirect_uri") String redirect_uri,
            @RequestParam(name="scope", required=false) String scope,
            @RequestParam(name="state", required=false) String state,
            ModelMap model) {
        List<String> errors = new ArrayList<>();
        AuthorizeRestRequest request = AuthorizeRestRequest.builder()
                .response_type(response_type)
                .response_mode(response_mode)
                .client_id(client_id)
                .redirect_uri(redirect_uri)
                .scope(scope)
                .state(state)
                .email("")
                .password("")
                .build();
        errors.addAll(request.validate());
        model.addAttribute("authorize", request);
        model.addAttribute("errors", errors);

        return "login";
    }

    @PostMapping(value = "/authorize")
    public String postAuthorization(@ModelAttribute AuthorizeRestRequest request,
                                    ModelMap model) {
        List<String> errors = new ArrayList<>();
        if (Strings.isNullOrEmpty(request.getEmail())) {
            errors.add("Email is required");
        }
        if (Strings.isNullOrEmpty(request.getPassword())) {
            errors.add("Password is required");
        }

        errors.addAll(request.validate());
        if (errors.size() > 0) {
            model.addAttribute("authorize", request);
            model.addAttribute("errors", errors);
            return "login";
        }

        String userId = authService.authenticate(request.getEmail(), request.getPassword());

        System.out.println("LOL!!");
        return "login";
    }
}
