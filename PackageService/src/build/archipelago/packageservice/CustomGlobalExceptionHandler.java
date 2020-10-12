package build.archipelago.packageservice;

import build.archipelago.common.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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

}
