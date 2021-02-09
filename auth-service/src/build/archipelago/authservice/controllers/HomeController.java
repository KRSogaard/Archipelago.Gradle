package build.archipelago.authservice.controllers;

import build.archipelago.authservice.models.AuthorizeRequest;
import build.archipelago.authservice.models.rest.AuthorizeRestRequest;
import build.archipelago.authservice.services.auth.AuthService;
import build.archipelago.authservice.services.auth.models.CodeResponse;
import build.archipelago.authservice.services.users.UserService;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import build.archipelago.authservice.services.users.exceptions.*;

import javax.servlet.http.*;

import static build.archipelago.authservice.controllers.Constants.AUTH_COOKIE;

@Controller
@Slf4j
public class HomeController {

    private AuthService authService;
    private UserService userService;

    public HomeController(AuthService authService,
                          UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @RequestMapping(value = "/")
    public String index() {
        return "index";
    }

    @RequestMapping(value = "/login")
    public String login(
            @CookieValue(value = AUTH_COOKIE, required = false) String authCookieToken,
            @RequestParam(name="response_type") String response_type,
            @RequestParam(name="response_mode", required=false) String response_mode,
            @RequestParam(name="client_id") String client_id,
            @RequestParam(name="redirect_uri") String redirect_uri,
            @RequestParam(name="scope", required=false) String scope,
            @RequestParam(name="state", required=false) String state,
            @RequestParam(name="nonce", required=false) String nonce,
            Model model,
            HttpServletResponse httpServletResponse) {
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
        if (errors.size() > 0) {
            // This is not a login error, but a request error no need to show login for user
            model.addAttribute("errors", errors);
            return "autherror";
        }

        if (!Strings.isNullOrEmpty(authCookieToken)) {
            try {
                String userId = authService.getUserFromAuthCookie(authCookieToken);
                String authToken = authService.createAuthToken(userId, request.toInternal());

                // Create a new auth cookie for the user
                CodeResponse authCookie = authService.createAuthCookie(userId);
                Cookie cookie = new Cookie(AUTH_COOKIE, authCookie.getCode());
                httpServletResponse.addCookie(cookie);
                return "redirect:" + buildRedirectUrl(request.toInternal(), authToken);

            } catch (UserNotFoundException exp) {
                log.info("The user had an auth cookie, but it was not valid");
            }
        }

        model.addAttribute("authorize", request);
        model.addAttribute("errors", errors);

        return "login";
    }

    @PostMapping(value = "/login")
    public String postLogin(@ModelAttribute AuthorizeRestRequest request,
                                    ModelMap model,
                                    HttpServletResponse httpServletResponse) {
        List<String> errors = new ArrayList<>();
        model.addAttribute("authorize", request);
        if (Strings.isNullOrEmpty(request.getEmail())) {
            errors.add("Email is required");
        }
        if (Strings.isNullOrEmpty(request.getPassword())) {
            errors.add("Password is required");
        }

        errors.addAll(request.validate());
        if (errors.size() > 0) {
            // This is not a login error, but a request error no need to show login for user
            model.addAttribute("errors", errors);
            return "autherror";
        }

        try {
            String userId = userService.authenticate(request.getEmail(), request.getPassword());
            String authToken = authService.createAuthToken(userId, request.toInternal());


            CodeResponse authCookie = authService.createAuthCookie(userId);
            Cookie cookie = new Cookie(AUTH_COOKIE, authCookie.getCode());
            httpServletResponse.addCookie(cookie);
            return "redirect:" + buildRedirectUrl(request.toInternal(), authToken);
        } catch (UserNotFoundException e) {
            errors.add("Incorrect username or password.");
            model.addAttribute("errors", errors);
            return "login";
        }
    }

    private String buildRedirectUrl(AuthorizeRequest request, String authToken) {
        StringBuilder redirectUrlBuilder = new StringBuilder();
        redirectUrlBuilder.append(request.getRedirectUri());
        if (!Strings.isNullOrEmpty(request.getResponseMode()) ||
                request.getResponseMode().equalsIgnoreCase("query")) {
            redirectUrlBuilder.append("?");
        } else {
            redirectUrlBuilder.append("#");
        }
        redirectUrlBuilder.append("code=").append(authToken);
        if (!Strings.isNullOrEmpty(request.getState())) {
            redirectUrlBuilder.append("&state=").append(request.getState());
        }
        return redirectUrlBuilder.toString();
    }

    @RequestMapping(value = "/auth-error")
    public String error() {
        return "error";
    }

    @RequestMapping(value = "/device")
    public String device(
            @CookieValue(value = AUTH_COOKIE, required = false) String authCookieToken,
            @RequestParam(name="user_code") String user_code) {
        
        return "device";
    }
}
