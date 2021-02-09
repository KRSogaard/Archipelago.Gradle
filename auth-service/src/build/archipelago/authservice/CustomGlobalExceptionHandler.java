package build.archipelago.authservice;

import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.authservice.services.clients.eceptions.ClientNotFoundException;
import build.archipelago.authservice.services.keys.exceptions.KeyNotFoundException;
import build.archipelago.authservice.utils.ResponseUtil;
import build.archipelago.common.rest.models.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@ControllerAdvice
public class CustomGlobalExceptionHandler extends RFC7807ExceptionHandler {

    @ExceptionHandler(UnauthorizedAuthTokenException.class)
    public ResponseEntity<String> springHandleUnauthorizedAuthTokenException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception UnauthorizedAuthTokenException: " + ex.getMessage());
        return ResponseUtil.unauthorizedAuthToken();
    }

    @ExceptionHandler(KeyNotFoundException.class)
    public ResponseEntity<String> springHandleKeyNotFoundException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception KeyNotFoundException: " + ex.getMessage());
        return ResponseUtil.unauthorizedAuthToken();
    }

    @ExceptionHandler(TokenNotValidException.class)
    public ResponseEntity<String> springHandleTokenNotValidException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception TokenNotValidException: " + ex.getMessage());
        return ResponseUtil.unauthorizedAuthToken();
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<String> springHandleClientNotFoundException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception ClientNotFoundException: " + ex.getMessage());
        return ResponseUtil.unauthorizedAuthToken();
    }

    @ExceptionHandler(ClientSecretRequiredException.class)
    public ResponseEntity<String> springHandleClientSecretRequiredException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception ClientSecretRequiredException: " + ex.getMessage());
        return ResponseUtil.unauthorizedAuthToken();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleIllegalArgumentException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception IllegalArgumentException: " + ex.getMessage());
        return this.createResponse(req, CommonExceptionHandler.from((IllegalArgumentException) ex));
    }
}
