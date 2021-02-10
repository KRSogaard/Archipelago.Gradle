package build.archipelago.authservice.schedules;

import build.archipelago.authservice.services.auth.*;
import lombok.extern.slf4j.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;

@Component
@Slf4j
public class CleanAuthCodesSchedule {

    private AuthService authService;

    public CleanAuthCodesSchedule(AuthService authService) {
        this.authService = authService;
    }

    // Every 12 hours, or 10 sek after start up
    @Scheduled(fixedRate = 43200000, initialDelay = 10000)
    public void clearCodes() {
        log.info("Clearing expires auth codes");
        try {
            authService.clearExpiredCodes();
        } catch (Exception exp) {
            log.error("Clearing auth codes failed", exp);
        }
    }
}
