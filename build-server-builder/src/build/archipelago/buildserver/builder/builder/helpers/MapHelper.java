package build.archipelago.buildserver.builder.builder.helpers;

import java.util.*;

public class MapHelper {
    public static <T> Map<T, Boolean> createLookUpMap(List<T> values) {
        Map<T, Boolean> lookupMap = new HashMap<>();
        values.forEach(v -> lookupMap.put(v, true));
        return lookupMap;
    }
}
