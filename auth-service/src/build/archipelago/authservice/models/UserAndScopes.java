package build.archipelago.authservice.models;

import lombok.*;

import java.util.List;

@Builder
@Data
public class UserAndScopes {
    private String userId;
    private List<String> scopes;
}
