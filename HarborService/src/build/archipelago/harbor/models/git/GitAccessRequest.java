package build.archipelago.harbor.models.git;

import lombok.*;

@Data
@Builder
public class GitAccessRequest {
    private String type;
    private String username;
    private String accessToken;
}
