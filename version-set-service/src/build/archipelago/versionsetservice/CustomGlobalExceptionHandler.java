package build.archipelago.versionsetservice;

import build.archipelago.common.rest.models.errors.*;
import build.archipelago.packageservice.client.PackageExceptionHandler;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.versionsetservice.client.VersionSetExceptionHandler;
import build.archipelago.versionsetservice.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
@Slf4j
public class CustomGlobalExceptionHandler extends RFC7807ExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleIllegalArgumentException(HttpServletRequest req, Exception ex) {
        return this.createResponse(req, CommonExceptionHandler.from((IllegalArgumentException) ex));
    }

    @ExceptionHandler(VersionSetDoseNotExistsException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleVersionSetDoseNotExistsException(HttpServletRequest req, Exception ex) {
        VersionSetDoseNotExistsException exp = (VersionSetDoseNotExistsException) ex;
        return this.createResponse(req, VersionSetExceptionHandler.from(exp));
    }

    @ExceptionHandler(VersionSetExistsException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleVersionSetExistsException(HttpServletRequest req, Exception ex) {
        return this.createResponse(req, VersionSetExceptionHandler.from((VersionSetExistsException) ex));
    }

    @ExceptionHandler(PackageNotFoundException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandlePackageNotFoundException(HttpServletRequest req, Exception ex) {
        return this.createResponse(req, PackageExceptionHandler.from((PackageNotFoundException) ex));
    }

    @ExceptionHandler(MissingTargetPackageException.class)
    public ResponseEntity<ProblemDetailRestResponse> stringHandleMissingTargetPackageException(HttpServletRequest req, Exception ex) {
        return this.createResponse(req, VersionSetExceptionHandler.from((MissingTargetPackageException) ex));
    }
}
