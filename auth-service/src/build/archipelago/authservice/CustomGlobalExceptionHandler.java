package build.archipelago.authservice;

import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.authservice.models.rest.*;
import build.archipelago.authservice.services.auth.exceptions.*;
import build.archipelago.authservice.services.clients.eceptions.*;
import build.archipelago.authservice.services.keys.exceptions.*;
import build.archipelago.authservice.utils.*;
import build.archipelago.common.rest.models.errors.*;
import lombok.extern.slf4j.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.*;

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

    @ExceptionHandler(InvalidGrantTypeException.class)
    public ResponseEntity<String> springHandleInvalidGrantTypeException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception InvalidGrantTypeException: " + ex.getMessage());
        return ResponseUtil.invalidGrantType();
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<String> springHandleTokenExpiredExceptionn(HttpServletRequest req, Exception ex) {
        log.warn("Got exception TokenExpiredException: " + ex.getMessage());

        return ResponseUtil.respons(HttpStatus.BAD_REQUEST, ErrorRestResponse.builder()
                .error("expired_token")
                .error_description("Invalid grant type")
                .build());
    }

    @ExceptionHandler(AuthorizationPendingException.class)
    public ResponseEntity<String> springHandleAuthorizationPendingException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception AuthorizationPendingException: " + ex.getMessage());

        return ResponseUtil.respons(HttpStatus.FORBIDDEN, ErrorRestResponse.builder()
                .error("authorization_pending")
                .error_description("The user has yet to approve the device code")
                .build());
    }

    @ExceptionHandler(DeviceCodeNotFoundException.class)
    public ResponseEntity<String> springHandleDeviceCodeNotFoundException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception AuthorizationPendingException: " + ex.getMessage());

        return ResponseUtil.respons(HttpStatus.FORBIDDEN, ErrorRestResponse.builder()
                .error("invalid_grant")
                .error_description("Invalid grant")
                .build());
    }



    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleIllegalArgumentException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception IllegalArgumentException: " + ex.getMessage());
        return this.createResponse(req, CommonExceptionHandler.from((IllegalArgumentException) ex));
    }
}
