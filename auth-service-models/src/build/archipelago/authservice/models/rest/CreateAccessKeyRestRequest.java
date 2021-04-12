package build.archipelago.authservice.models.rest;

import lombok.*;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class CreateAccessKeyRestRequest {
    private String userId;
    private String scope;
}
