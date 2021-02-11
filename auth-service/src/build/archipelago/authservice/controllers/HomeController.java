package build.archipelago.authservice.controllers;

import build.archipelago.authservice.models.*;
import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.authservice.models.rest.*;
import build.archipelago.authservice.services.auth.*;
import build.archipelago.authservice.services.auth.models.*;
import build.archipelago.authservice.services.users.*;
import build.archipelago.authservice.services.users.exceptions.*;
import build.archipelago.authservice.services.users.models.*;
import com.google.common.base.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.ui.*;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.*;
import java.util.*;

import static build.archipelago.authservice.controllers.Constants.*;

@Controller
@Slf4j
public class HomeController {

    private int cookieMaxAge;
    private AuthService authService;
    private UserService userService;

    public HomeController(AuthService authService,
                          UserService userService,
                          @Value("${token.age.authCookie}") int cookieMaxAge) {
        this.authService = authService;
        this.userService = userService;
        this.cookieMaxAge = cookieMaxAge;
    }

//    @GetMapping(value = "/")
//    public String index() {
//        return "index";
//    }
//
//    @GetMapping(value = "/login")
//    public String login(
//            @CookieValue(value = AUTH_COOKIE, required = false) String authCookieToken,
//            @RequestParam(name="response_type") String response_type,
//            @RequestParam(name="response_mode", required=false) String response_mode,
//            @RequestParam(name="client_id") String client_id,
//            @RequestParam(name="redirect_uri") String redirect_uri,
//            @RequestParam(name="scope", required=false) String scope,
//            @RequestParam(name="state", required=false) String state,
//            @RequestParam(name="nonce", required=false) String nonce,
//            Model model,
//            HttpServletResponse httpServletResponse) {
//        List<String> errors = new ArrayList<>();
//        AuthorizeRestRequest request = AuthorizeRestRequest.builder()
//                .response_type(response_type)
//                .response_mode(response_mode)
//                .client_id(client_id)
//                .redirect_uri(redirect_uri)
//                .scope(scope)
//                .state(state)
//                .nonce(nonce)
//                .email("")
//                .password("")
//                .build();
//        errors.addAll(request.validate());
//        if (errors.size() > 0) {
//            // This is not a login error, but a request error no need to show login for user
//            model.addAttribute("errors", errors);
//            return "autherror";
//        }
//
//        if (!Strings.isNullOrEmpty(authCookieToken)) {
//            try {
//                String userId = authService.getUserFromAuthCookie(authCookieToken);
//                String authToken = authService.createAuthToken(userId, request.toInternal());
//
//                // Create a new auth cookie for the user
//                CodeResponse authCookie = authService.createAuthCookie(userId);
//                Cookie cookie = new Cookie(AUTH_COOKIE, authCookie.getCode());
//                httpServletResponse.addCookie(cookie);
//                cookie.setMaxAge(cookieMaxAge);
//                return "redirect:" + buildRedirectUrl(request.toInternal(), authToken);
//
//            } catch (UserNotFoundException | TokenExpiredException | TokenNotFoundException exp) {
//                log.info("The user had an auth cookie, but it was not valid. Got '{}'", exp.getClass().getName());
//                Cookie cookie = new Cookie(AUTH_COOKIE, "");
//                cookie.setMaxAge(0);
//                httpServletResponse.addCookie(cookie);
//            }
//        }
//
//        model.addAttribute("authorize", request);
//        model.addAttribute("errors", errors);
//
//        return "login";
//    }
//
//    @PostMapping(value = "/login")
//    public String postLogin(@ModelAttribute AuthorizeRestRequest request,
//                            ModelMap model,
//                            HttpServletResponse httpServletResponse) {
//        List<String> errors = new ArrayList<>();
//        model.addAttribute("authorize", request);
//        if (Strings.isNullOrEmpty(request.getEmail())) {
//            errors.add("Email is required");
//        }
//        if (Strings.isNullOrEmpty(request.getPassword())) {
//            errors.add("Password is required");
//        }
//
//        errors.addAll(request.validate());
//        if (errors.size() > 0) {
//            // This is not a login error, but a request error no need to show login for user
//            model.addAttribute("errors", errors);
//            return "autherror";
//        }
//
//        try {
//            String userId = userService.authenticate(request.getEmail(), request.getPassword());
//            String authToken = authService.createAuthToken(userId, request.toInternal());
//
//            CodeResponse authCookie = authService.createAuthCookie(userId);
//            Cookie cookie = new Cookie(AUTH_COOKIE, authCookie.getCode());
//            httpServletResponse.addCookie(cookie);
//            return "redirect:" + buildRedirectUrl(request.toInternal(), authToken);
//        } catch (UserNotFoundException e) {
//            errors.add("Incorrect username or password.");
//            model.addAttribute("errors", errors);
//            return "login";
//        }
//    }
//
//    private String buildRedirectUrl(AuthorizeRequest request, String authToken) {
//        StringBuilder redirectUrlBuilder = new StringBuilder();
//        redirectUrlBuilder.append(request.getRedirectUri());
//        if (!Strings.isNullOrEmpty(request.getResponseMode()) ||
//                request.getResponseMode().equalsIgnoreCase("query")) {
//            redirectUrlBuilder.append("?");
//        } else {
//            redirectUrlBuilder.append("#");
//        }
//        redirectUrlBuilder.append("code=").append(authToken);
//        if (!Strings.isNullOrEmpty(request.getState())) {
//            redirectUrlBuilder.append("&state=").append(request.getState());
//        }
//        return redirectUrlBuilder.toString();
//    }
//
//    @GetMapping(value = "/auth-error")
//    public String error() {
//        return "error";
//    }
//
//    @GetMapping(value = "/device")
//    public String device(@RequestParam(name="user_code", required = false) String user_code,
//                         ModelMap model) {
//        model.addAttribute("deviceRequest", new DeviceRestRequest() {{
//            setUser_code(user_code);
//        }});
//        return "device";
//    }
//
//    @PostMapping(value = "/device")
//    public String devicePost(
//            @CookieValue(value = AUTH_COOKIE, required = false) String authCookieToken,
//            @RequestParam(name="user_code") String user_code,
//            @RequestParam(name="email", required=false) String email,
//            @RequestParam(name="password", required=false) String password,
//            ModelMap model,
//            HttpServletResponse httpServletResponse) {
//        String userCode = user_code.toUpperCase();
//        model.addAttribute("deviceRequest", new DeviceRestRequest() {{
//            setUser_code(user_code);
//            setEmail(email);
//        }});
//
//
//        DeviceCode code;
//        try {
//            code = authService.getDeviceCode(user_code.toUpperCase());
//        } catch (TokenNotFoundException | TokenExpiredException e) {
//            model.addAttribute("error", "User code was not found");
//            return "device";
//        }
//
//        String userId = null;
//        if (!Strings.isNullOrEmpty(authCookieToken)) {
//            try {
//                userId = authService.getUserFromAuthCookie(authCookieToken);
//            } catch (UserNotFoundException | TokenExpiredException | TokenNotFoundException e) {
//                log.info("Request had an invalid auth cookie code, removing it");
//                Cookie cookie = new Cookie(AUTH_COOKIE, "");
//                cookie.setMaxAge(0);
//                httpServletResponse.addCookie(cookie);
//            }
//        } else if (!Strings.isNullOrEmpty(email) && !Strings.isNullOrEmpty(password)) {
//            try {
//                userId = userService.authenticate(email, password);
//
//                CodeResponse authCookie = authService.createAuthCookie(userId);
//                Cookie cookie = new Cookie(AUTH_COOKIE, authCookie.getCode());
//                httpServletResponse.addCookie(cookie);
//            } catch (UserNotFoundException e) {
//                model.addAttribute("error", "Invalid email or password");
//            }
//        }
//        if (Strings.isNullOrEmpty(userId)) {
//            return "deviceLogin";
//        }
//
//        authService.updateDeviceCode(userCode, userId);
//
//        return "deviceSuccess";
//    }
//
//    @GetMapping("/create-account")
//    public String createAccount(
//            @RequestParam(name="response_type") String response_type,
//            @RequestParam(name="response_mode", required=false) String response_mode,
//            @RequestParam(name="client_id") String client_id,
//            @RequestParam(name="redirect_uri") String redirect_uri,
//            @RequestParam(name="scope", required=false) String scope,
//            @RequestParam(name="state", required=false) String state,
//            @RequestParam(name="nonce", required=false) String nonce,
//            Model model) {
//        CreateAccountModel accountModel = CreateAccountModel.builder()
//                .response_type(response_type)
//                .response_mode(response_mode)
//                .client_id(client_id)
//                .redirect_uri(redirect_uri)
//                .scope(scope)
//                .state(state)
//                .nonce(nonce)
//                .build();
//
//        model.addAttribute("model", accountModel);
//        model.addAttribute("errors", new ArrayList<>());
//
//        return "create-account";
//    }
//
//
//    @PostMapping("/create-account")
//    public String createAccountPost(@ModelAttribute CreateAccountModel request,
//                                    Model model,
//                                    HttpServletResponse httpServletResponse) {
//        List<String> errors = new ArrayList<>();
//
//        if (Strings.isNullOrEmpty(request.getName())) {
//            errors.add("Name is required");
//        }
//        if (Strings.isNullOrEmpty(request.getEmail())) {
//            errors.add("Email is required");
//        }
//        if (Strings.isNullOrEmpty(request.getPassword())) {
//            errors.add("Password is required");
//        }
//        if (Strings.isNullOrEmpty(request.getPassword_repeat())) {
//            errors.add("Please repeat your password");
//        }
//        if (!Strings.isNullOrEmpty(request.getPassword()) && !Strings.isNullOrEmpty(request.getPassword_repeat()) &&
//                !request.getPassword().equals(request.getPassword_repeat())) {
//            errors.add("Passwords did not match");
//        }
//
//        try {
//            String userId = userService.createUser(UserModel.builder()
//                    .name(request.getName())
//                    .email(request.getEmail())
//                    .password(request.getPassword())
//                    .build());
//
//            AuthorizeRequest authorizeRequest = AuthorizeRequest.builder()
//                    .responseType(request.getResponse_type())
//                    .responseMode(request.getResponse_mode())
//                    .clientId(request.getClient_id())
//                    .redirectUri(request.getRedirect_uri())
//                    .scope(request.getScope())
//                    .state(request.getState())
//                    .build();
//            String authToken = authService.createAuthToken(userId, authorizeRequest);
//
//            CodeResponse authCookie = authService.createAuthCookie(userId);
//            Cookie cookie = new Cookie(AUTH_COOKIE, authCookie.getCode());
//            httpServletResponse.addCookie(cookie);
//            return "redirect:" + buildRedirectUrl(authorizeRequest, authToken);
//
//
//
//        } catch (UserExistsException e) {
//            errors.add(String.format("An user with the email '%s' already exits", request.getEmail()));
//        }
//
//
//        model.addAttribute("model", request);
//        model.addAttribute("errors", errors);
//        return "create-account";
//    }
}
