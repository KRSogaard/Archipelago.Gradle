package build.archipelago.harbor;

import build.archipelago.authservice.client.*;
import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.buildserver.api.client.BuildsExceptionHandler;
import build.archipelago.buildserver.models.exceptions.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.rest.models.errors.*;
import build.archipelago.packageservice.client.PackageExceptionHandler;
import build.archipelago.packageservice.exceptions.*;
import build.archipelago.versionsetservice.client.VersionSetExceptionHandler;
import build.archipelago.versionsetservice.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@ControllerAdvice
public class CustomGlobalExceptionHandler extends RFC7807ExceptionHandler {

    @ExceptionHandler(PackageNotFoundException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandlePackageNotFoundException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception PackageNotFoundException: " + ex.getMessage());
        return this.createResponse(req, PackageExceptionHandler.from((PackageNotFoundException) ex));
    }

    @ExceptionHandler(PackageExistsException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandlePackageExistsException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception PackageExistsException: " + ex.getMessage());
        return this.createResponse(req, PackageExceptionHandler.from((PackageExistsException) ex));
    }

    @ExceptionHandler(VersionSetDoseNotExistsException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleVersionSetDoseNotExistsException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception VersionSetDoseNotExistsException: " + ex.getMessage());
        return this.createResponse(req, VersionSetExceptionHandler.from((VersionSetDoseNotExistsException) ex));
    }

    @ExceptionHandler(VersionSetExistsException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleVersionSetExistsException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception VersionSetExistsException: " + ex.getMessage());
        return this.createResponse(req, VersionSetExceptionHandler.from((VersionSetExistsException) ex));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleIllegalArgumentException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception IllegalArgumentException: " + ex.getMessage());
        return this.createResponse(req, CommonExceptionHandler.from((IllegalArgumentException) ex));
    }

    @ExceptionHandler(StageLogNotFoundException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleStageLogNotFoundException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception StageLogNotFoundException: " + ex.getMessage());
        return this.createResponse(req, BuildsExceptionHandler.from((StageLogNotFoundException) ex));
    }

    @ExceptionHandler(PackageLogNotFoundException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandlePackageLogNotFoundException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception PackageLogNotFoundException: " + ex.getMessage());
        return this.createResponse(req, BuildsExceptionHandler.from((PackageLogNotFoundException) ex));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleUserNotFoundException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception UserNotFoundException: " + ex.getMessage());
        return this.createResponse(req, AuthExceptionHandler.from((UserNotFoundException) ex));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleUnauthorizedException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception UserNotFoundException: " + ex.getMessage());
        return this.createResponse(req, CommonExceptionHandler.from((UnauthorizedException) ex));
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleTokenExpiredException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception UserNotFoundException: " + ex.getMessage());
        return this.createResponse(req, AuthExceptionHandler.from((TokenExpiredException) ex));
    }

    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleTokenNotFoundException(HttpServletRequest req, Exception ex) {
        log.warn("Got exception UserNotFoundException: " + ex.getMessage());
        return this.createResponse(req, AuthExceptionHandler.from((TokenNotFoundException) ex));
    }
}
