package build.archipelago.harbor.controllers;

import build.archipelago.authservice.client.AuthClient;
import build.archipelago.authservice.models.AccessKey;
import build.archipelago.authservice.models.exceptions.AccessKeyNotFound;
import build.archipelago.authservice.models.rest.AccessKeyRestResponse;
import build.archipelago.authservice.models.rest.AccessKeysRestResponse;
import build.archipelago.harbor.filters.AccountIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accessKeys")
@CrossOrigin(origins = "*")
@Slf4j
public class AccessKeysController {
    private AuthClient authClient;

    public AccessKeysController(AuthClient authClient) {
        this.authClient = authClient;
    }

    @PostMapping()
    public AccessKeyRestResponse createAccessKey(
            @RequestAttribute(AccountIdFilter.AccountIdKey) String accountId) {
        AccessKey key = authClient.createAccessKey(accountId);
        return AccessKeyRestResponse.from(key);
    }

    @GetMapping()
    public AccessKeysRestResponse getAccessKeys(
            @RequestAttribute(AccountIdFilter.AccountIdKey) String accountId) {
        List<AccessKey> keys = authClient.getAccessKeys(accountId);
        return AccessKeysRestResponse.from(keys);
    }

    @PostMapping("/{username}/{token}")
    public AccessKeyRestResponse verifyAccessKey(
            @PathVariable("username") String username,
            @PathVariable("token") String token) throws AccessKeyNotFound {
        AccessKey accessKey = authClient.verifyAccessKey(username, token);
        if (!accessKey.getToken().equals(token)) {
            throw new AccessKeyNotFound();
        }
        return AccessKeyRestResponse.from(AccessKey.builder()
                .accountId(accessKey.getAccountId())
                .username(accessKey.getUsername())
                .created(accessKey.getCreated())
                .lastUsed(accessKey.getLastUsed())
                .scope(accessKey.getScope())
                .build());
    }

    @DeleteMapping("/{username}")
    public void deleteAccessKey(
            @RequestAttribute(AccountIdFilter.AccountIdKey) String accountId,
            @PathVariable("username") String username) {

        authClient.deleteAccessKey(accountId, username);
    }
}
