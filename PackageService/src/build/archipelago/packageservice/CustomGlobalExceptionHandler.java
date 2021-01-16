package build.archipelago.packageservice;

import build.archipelago.account.common.exceptions.GitDetailsNotFound;
import build.archipelago.common.exceptions.PackageArtifactExistsException;
import build.archipelago.common.exceptions.PackageExistsException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@ControllerAdvice
public class CustomGlobalExceptionHandler {

    @ExceptionHandler(PackageNotFoundException.class)
    public void springHandlePackageNotFoundException(Exception ex, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.NOT_FOUND.value(), formatErrorMessage("PackageNotFound", ex.getMessage()));
    }

    @ExceptionHandler(PackageArtifactExistsException.class)
    public void springHandlePackageArtifactExistsException(Exception ex, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.CONFLICT.value(), formatErrorMessage("PackageArtifactExists", ex.getMessage()));
    }

    @ExceptionHandler(PackageExistsException.class)
    public void springHandlePackageExistsException(Exception ex, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.CONFLICT.value(), formatErrorMessage("PackageExists", ex.getMessage()));
    }

    @ExceptionHandler(GitDetailsNotFound.class)
    public void springHandleGitDetailsNotFound(Exception ex, HttpServletResponse response) throws IOException {
        log.error("The user did not have git details, so we could not execute the command");
        response.sendError(HttpStatus.BAD_REQUEST.value(), formatErrorMessage("GitDetailsNotFound", ex.getMessage()));
    }

    private String formatErrorMessage(String code, String message) {
        return String.format("{ \"code\": \"%s\", \"message\": \"%s\"}", code, message);
    }

}
