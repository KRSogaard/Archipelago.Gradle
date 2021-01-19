package build.archipelago.common.rest.models.errors;

import lombok.*;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Map;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ProblemDetailRestResponse {
    protected static com.fasterxml.jackson.databind.ObjectMapper objectMapper
            = new com.fasterxml.jackson.databind.ObjectMapper();
    public static final MediaType JSON_MEDIA_TYPE = MediaType.APPLICATION_PROBLEM_JSON;

    private final boolean error = true;
    private String type;
    private String title;
    private String detail;
    private Integer status;
    private String instance;
    private Map<String, Object> data;

    public static ProblemDetailRestResponse from(String body) {
        try {
            return objectMapper.readValue(body, ProblemDetailRestResponse.class);
        } catch (IOException exp) {
            throw new RuntimeException("The response body was not a RFC7807 standard", exp);
        }
    }
}
