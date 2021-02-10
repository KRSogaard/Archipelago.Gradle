package build.archipelago.authservice.services.keys;

import build.archipelago.authservice.services.keys.exceptions.KeyNotFoundException;
import build.archipelago.authservice.services.keys.models.*;

import java.util.*;

public interface KeyService {
    KeyDetails getSigningKey();
    List<JWKKey> getActiveKeys();
    KeyDetails getKey(String keyId) throws KeyNotFoundException;
}
