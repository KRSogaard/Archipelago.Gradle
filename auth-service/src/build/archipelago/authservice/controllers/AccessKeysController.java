package build.archipelago.authservice.controllers;

import build.archipelago.account.common.AccountService;
import build.archipelago.authservice.models.AccessKey;
import build.archipelago.authservice.models.exceptions.AccessKeyNotFound;
import build.archipelago.authservice.models.rest.AccessKeyRestResponse;
import build.archipelago.authservice.models.rest.AccessKeysRestResponse;
import build.archipelago.authservice.services.accessKeys.AccessKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accessKeys")
@Slf4j
public class AccessKeysController {

    private AccessKeyService accessKeyService;

    public AccessKeysController(AccessKeyService accessKeyService) {
        this.accessKeyService = accessKeyService;
    }

    @PostMapping("/{accountId}")
    public AccessKeyRestResponse createAccessKey(
            @PathVariable("accountId") String accountId) {
        AccessKey key = accessKeyService.createAccessKey(accountId, "");
        return AccessKeyRestResponse.from(key);
    }

    @GetMapping("/{accountId}")
    public AccessKeysRestResponse getAccessKeys(
            @PathVariable("accountId") String accountId) {
        List<AccessKey> keys = accessKeyService.getAccessKeys(accountId);
        return AccessKeysRestResponse.from(keys);
    }

    @PostMapping("/{username}/{token}")
    public AccessKeyRestResponse verifyAccessKey(
            @PathVariable("username") String username,
            @PathVariable("token") String token) throws AccessKeyNotFound {
        AccessKey accessKey = accessKeyService.getAccessKey(username);
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

    @DeleteMapping("/{accountId}/{username}")
    public void deleteAccessKey(
            @PathVariable("accountId") String accountId,
            @PathVariable("username") String username) {

        String[] split = username.split("\\.", 2);
        String verifiedUsername = accountId + "." + split[1];
        if (!username.equalsIgnoreCase(verifiedUsername)) {
            log.warn("Username did not match account id '{}' was provided, '{}' was calculated", username, verifiedUsername);
            throw new IllegalArgumentException("Username was not verified");
        }
        accessKeyService.deleteAccessKey(verifiedUsername);
    }
}
