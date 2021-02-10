package build.archipelago.authservice.services.clients.models;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class Client {
    private String clientId;
    private String clientSecret;
    private Instant created;
    private Instant updated;
    private List<String> allowedRedirects;
    private List<String> allowedScopes;
}
