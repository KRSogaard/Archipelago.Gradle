package build.archipelago.versionsetservice;

import build.archipelago.common.exceptions.MissingTargetPackageException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.exceptions.VersionSetExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

@ControllerAdvice
@Slf4j
public class CustomGlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public void springHandleIllegalArgumentException(Exception ex, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }

    @ExceptionHandler(VersionSetDoseNotExistsException.class)
    public void stringHandleVersionSetDoseNotExistsException(Exception ex, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    }

    @ExceptionHandler(VersionSetExistsException.class)
    public void stringHandleVersionSetExistsException(Exception ex, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.CONFLICT.value(), ex.getMessage());
    }

    @ExceptionHandler(PackageNotFoundException.class)
    public void stringHandlePackageNotFoundException(Exception ex, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.NOT_ACCEPTABLE.value(), ((PackageNotFoundException)ex).getPackageName());
    }

    @ExceptionHandler(MissingTargetPackageException.class)
    public void stringHandleMissingTargetPackageException(Exception ex, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.PRECONDITION_FAILED.value(), ex.getMessage());
    }
}
