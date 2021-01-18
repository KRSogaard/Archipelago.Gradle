package build.archipelago.common.rest.models.errors;

import org.springframework.http.HttpStatus;

public class CommonExceptionHandler {

    public static ProblemDetailRestResponse.ProblemDetailRestResponseBuilder from(IllegalArgumentException exp) {
        return ProblemDetailRestResponse.builder()
                .type("client/illegalArgument")
                .title("The provided data was not acceptable")
                .status(HttpStatus.NOT_ACCEPTABLE.value())
                .detail(exp.getMessage());
    }
}
