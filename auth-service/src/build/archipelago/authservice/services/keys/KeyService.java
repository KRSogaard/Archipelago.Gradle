package build.archipelago.authservice.services.keys;

import java.util.*;

public interface KeyService {
    KeyDetails getSigningKey();
    List<JWKKey> getActiveKeys();
}
