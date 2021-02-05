package build.archipelago.authservice.services.keys;

import java.util.*;

public interface KeyService {
    String createJWTToken(Map<String, Object> claims);
    List<JWKKey> getActiveKeys();
}
