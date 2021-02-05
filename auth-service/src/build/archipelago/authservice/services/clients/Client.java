package build.archipelago.authservice.services.clients;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class Client {
    private String clientId;
    private String clientSecret;
    private List<String> allowedRedirects;
    private List<String> allowedScopes;
}
