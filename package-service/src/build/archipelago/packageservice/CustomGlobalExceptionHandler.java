package build.archipelago.packageservice;

import build.archipelago.common.git.models.exceptions.GitDetailsNotFound;
import build.archipelago.common.git.models.exceptions.BranchNotFoundException;
import build.archipelago.common.git.models.exceptions.RepoNotFoundException;
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
        return this.createResponse(req, PackageExceptionHandler.from((PackageNotFoundException) ex));
    }

    @ExceptionHandler(PackageExistsException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandlePackageExistsException(HttpServletRequest req, Exception ex) {
        return this.createResponse(req, PackageExceptionHandler.from((PackageExistsException) ex));
    }

    @ExceptionHandler(GitDetailsNotFound.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleGitDetailsNotFound(HttpServletRequest req, Exception ex) {
        return this.createResponse(req, PackageExceptionHandler.from((GitDetailsNotFound) ex));
    }

    @ExceptionHandler(RepoNotFoundException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleRepoNotFoundException(HttpServletRequest req, Exception ex) {
        return this.createResponse(req, PackageExceptionHandler.from((RepoNotFoundException) ex));
    }

    @ExceptionHandler(BranchNotFoundException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleBranchNotFoundException(HttpServletRequest req, Exception ex) {
        return this.createResponse(req, PackageExceptionHandler.from((BranchNotFoundException) ex));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetailRestResponse> springHandleIllegalArgumentException(HttpServletRequest req, Exception ex) {
        return this.createResponse(req, CommonExceptionHandler.from((IllegalArgumentException) ex));
    }
}
