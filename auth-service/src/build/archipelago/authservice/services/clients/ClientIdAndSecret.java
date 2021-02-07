package build.archipelago.authservice.services.clients;

import lombok.*;

@Data
@Builder
public class ClientIdAndSecret {
    private String id;
    private String secret;
}
