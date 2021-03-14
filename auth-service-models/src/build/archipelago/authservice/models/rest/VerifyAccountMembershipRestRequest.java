package build.archipelago.authservice.models.rest;

import lombok.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class VerifyAccountMembershipRestRequest {
    private String accountId;
    private String userId;
}
