package build.archipelago.authservice.models.tokens;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessToken {
    private String iss;
    private String sub;
    private String scopes;
    private long iat;
    private String aug;
    private long exp;
    private String state;
}
