package build.archipelago.authservice.utils;

import com.google.common.base.Strings;

import java.util.*;
import java.util.stream.Collectors;

public class ScopeUtils {
    public static List<String> getScopes(String scopes) {
        return Arrays.stream(scopes.split(" ")).filter(Strings::isNullOrEmpty).collect(Collectors.toList());
    }
}
