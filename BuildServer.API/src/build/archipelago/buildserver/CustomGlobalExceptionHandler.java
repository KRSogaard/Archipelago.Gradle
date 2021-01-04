package build.archipelago.buildserver;

import build.archipelago.common.exceptions.PackageArtifactExistsException;
import build.archipelago.common.exceptions.PackageExistsException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

@ControllerAdvice
public class CustomGlobalExceptionHandler {

    @ExceptionHandler(PackageNotFoundException.class)
    public void springHandlePackageNotFoundException(Exception ex, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    }

    @ExceptionHandler(PackageArtifactExistsException.class)
    public void springHandlePackageArtifactExistsException(Exception ex, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.CONFLICT.value(), ex.getMessage());
    }

    @ExceptionHandler(PackageExistsException.class)
    public void springHandlePackageExistsException(Exception ex, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.CONFLICT.value(), ex.getMessage());
    }

    @ExceptionHandler(VersionSetDoseNotExistsException.class)
    public void springHandleVersionSetDoseNotExistsException(Exception ex, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.CONFLICT.value(), ex.getMessage());
    }
}
