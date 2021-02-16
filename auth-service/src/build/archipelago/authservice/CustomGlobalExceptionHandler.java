package build.archipelago.authservice;

import build.archipelago.authservice.client.*;
import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.authservice.models.rest.*;
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
    public ResponseEntity<ProblemDetailRestResponse> springHandleUnauthorizedAuthTokenException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception UnauthorizedAuthTokenException: " + ex.getMessage());
        return this.createResponse(req, AuthExceptionHandler.from((UnauthorizedAuthTokenException) ex));
    }

    @ExceptionHandler(KeyNotFoundException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleKeyNotFoundException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception KeyNotFoundException: " + ex.getMessage());
        return this.createResponse(req, AuthExceptionHandler.from((KeyNotFoundException) ex));
    }

    @ExceptionHandler(TokenNotValidException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleTokenNotValidException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception TokenNotValidException: " + ex.getMessage());
        return this.createResponse(req, AuthExceptionHandler.from((TokenNotValidException) ex));
    }

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleClientNotFoundException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception ClientNotFoundException: " + ex.getMessage());
        return this.createResponse(req, AuthExceptionHandler.from((ClientNotFoundException) ex));
    }

    @ExceptionHandler(ClientSecretRequiredException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleClientSecretRequiredException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception ClientSecretRequiredException: " + ex.getMessage());
        return this.createResponse(req, AuthExceptionHandler.from((ClientSecretRequiredException) ex));
    }

    @ExceptionHandler(InvalidGrantTypeException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleInvalidGrantTypeException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception InvalidGrantTypeException: " + ex.getMessage());
        return this.createResponse(req, AuthExceptionHandler.from((InvalidGrantTypeException) ex));
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleTokenExpiredExceptionn(HttpServletRequest req, Exception ex) {
        log.warn("Got exception TokenExpiredException: " + ex.getMessage());
        return this.createResponse(req, AuthExceptionHandler.from((TokenExpiredException) ex));
    }

    @ExceptionHandler(AuthorizationPendingException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleAuthorizationPendingException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception AuthorizationPendingException: " + ex.getMessage());
        return this.createResponse(req, AuthExceptionHandler.from((AuthorizationPendingException) ex));
    }

    @ExceptionHandler(DeviceCodeNotFoundException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleDeviceCodeNotFoundException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception UserNotFoundException: " + ex.getMessage());
        return this.createResponse(req, AuthExceptionHandler.from((DeviceCodeNotFoundException) ex));
    }

    // Non OAuth exceptions
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleUserNotFoundException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception UserNotFoundException: " + ex.getMessage());
        return this.createResponse(req, AuthExceptionHandler.from((UserNotFoundException) ex));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleIllegalArgumentException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception IllegalArgumentException: " + ex.getMessage());
        return this.createResponse(req, CommonExceptionHandler.from((IllegalArgumentException) ex));
    }
}
