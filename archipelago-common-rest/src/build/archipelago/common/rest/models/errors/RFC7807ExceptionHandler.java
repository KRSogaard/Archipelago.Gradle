package build.archipelago.common.rest.models.errors;

import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

public abstract class RFC7807ExceptionHandler {

    protected  <T extends Exception> ResponseEntity<ProblemDetailRestResponse> createResponse(HttpServletRequest req,
                                                                                           ProblemDetailRestResponse.ProblemDetailRestResponseBuilder builder) {
        ProblemDetailRestResponse detail = builder
                .instance(req.getRequestURI())
                .build();
        return ResponseEntity.status(detail.getStatus())
                .contentType(ProblemDetailRestResponse.JSON_MEDIA_TYPE)
                .body(detail);
    }
}
