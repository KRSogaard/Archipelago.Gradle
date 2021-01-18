package build.archipelago.packageservice;

import build.archipelago.account.common.exceptions.GitDetailsNotFound;
import build.archipelago.common.rest.models.errors.*;
import build.archipelago.packageservice.client.PackageExceptionHandler;
import build.archipelago.packageservice.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
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

    @ExceptionHandler(GitDetailsNotFound.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleGitDetailsNotFound(HttpServletRequest req, Exception ex) {
        return createResponse(req, ProblemDetailRestResponse.builder()
                        .type("git/detailsNotFound")
                        .title("No git details was found for the account")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(ex.getMessage()));
    }
}
