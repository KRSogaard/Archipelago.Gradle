package build.archipelago.authservice.controllers;

import lombok.extern.slf4j.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Slf4j
public class AuthController {

    @GetMapping("/test")
    public String test() {
        return "Pass";
    }
}
