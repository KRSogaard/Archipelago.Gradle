package build.archipelago.authservice.models.rest;

import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserAccountsRestResult {
    private List<String> accounts;
}
