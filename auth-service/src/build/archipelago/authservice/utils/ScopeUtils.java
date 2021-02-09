package build.archipelago.authservice.utils;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ScopeUtils {
    public static List<String> getScopes(String scopes) {
        if (Strings.isNullOrEmpty(scopes)) {
            return new ArrayList<>();
        }
        return Arrays.stream(scopes.split(" ")).filter(Strings::isNullOrEmpty).collect(Collectors.toList());
    }

    public static List<String> ensureNoNewScopes(List<String> allowedScopes, List<String> requestedScopes) {
        Map<String, Boolean> allowed = new HashMap<>();
        for (String c : allowedScopes) {
            allowed.put(c.toLowerCase(), true);
        }

        List<String> newScopes = new ArrayList<>();
        for (String s : requestedScopes) {
            if (allowed.containsKey(s.toLowerCase())) {
                newScopes.add(s);
            } else {
                log.warn("Request for scope '{}' was was not in the list of allowed scopes", s);
            }
        }
        return newScopes;
    }
}
