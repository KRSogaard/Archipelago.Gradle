package build.archipelago.versionsetservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class HealthController {

    public static final String HEALTHY_REPLY = "Healthy";
    public static final String HEALTH_PATH = "/health-check";

    @GetMapping(value = {HEALTH_PATH})
    @ResponseStatus(HttpStatus.OK)
    public String doHealthCheck() {
        return HEALTHY_REPLY;
    }
}
