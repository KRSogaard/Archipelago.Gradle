package build.archipelago.common.rest.models.errors;

import build.archipelago.common.exceptions.*;
import org.springframework.http.HttpStatus;

public class CommonExceptionHandler {

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(IllegalArgumentException exp) {
        return ProblemDetailRestResponse.builder()
                .error("client/illegalArgument")
                .title("The provided data was not acceptable")
                .status(HttpStatus.NOT_ACCEPTABLE.value())
                .detail(exp.getMessage());
    }

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(UnauthorizedException ex) {
        return ProblemDetailRestResponse.builder()
                .error("unauthorized")
                .title("The request was not authorized")
                .status(HttpStatus.UNAUTHORIZED.value())
                .detail("The request was not authorized");
    }
}
