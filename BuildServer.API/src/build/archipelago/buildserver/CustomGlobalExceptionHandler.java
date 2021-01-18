package build.archipelago.buildserver;

import build.archipelago.common.rest.models.errors.*;
import build.archipelago.packageservice.client.PackageExceptionHandler;
import build.archipelago.packageservice.exceptions.*;
import build.archipelago.versionsetservice.client.VersionSetExceptionHandler;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@ControllerAdvice
public class CustomGlobalExceptionHandler extends RFC7807ExceptionHandler {

    @ExceptionHandler(PackageNotFoundException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandlePackageNotFoundException(HttpServletRequest req, Exception ex) throws IOException {
        return createResponse(req, PackageExceptionHandler.from((PackageNotFoundException)ex));
    }

    @ExceptionHandler(PackageExistsException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandlePackageExistsException(HttpServletRequest req, Exception ex) {
        return createResponse(req, PackageExceptionHandler.from((PackageExistsException)ex));
    }

    @ExceptionHandler(VersionSetDoseNotExistsException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleVersionSetDoseNotExistsException(HttpServletRequest req, Exception ex) {
        return createResponse(req, VersionSetExceptionHandler.from((VersionSetDoseNotExistsException)ex));
    }
}
